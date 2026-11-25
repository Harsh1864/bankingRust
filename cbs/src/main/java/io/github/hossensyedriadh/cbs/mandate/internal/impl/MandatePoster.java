package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.account.api.enumerator.AccountStatus;
import io.github.hossensyedriadh.cbs.account.api.model.AccountBalanceView;
import io.github.hossensyedriadh.cbs.account.api.model.AccountView;
import io.github.hossensyedriadh.cbs.account.api.service.AccountBalanceService;
import io.github.hossensyedriadh.cbs.account.api.service.AccountService;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionFailedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionPostedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateStatusChangedEvent;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import io.github.hossensyedriadh.cbs.mandate.internal.outcome.CollectionOutcome;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateExecutionRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.PaymentMandateRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.util.AmountDecision;
import io.github.hossensyedriadh.cbs.mandate.internal.util.BankHolidayCalendar;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateAmounts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateIdempotency;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTexts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTransitions;
import io.github.hossensyedriadh.cbs.mandate.internal.util.ScheduleCalculator;
import io.github.hossensyedriadh.cbs.transaction.api.command.ChargeFeeCommand;
import io.github.hossensyedriadh.cbs.transaction.api.command.CreateTransferCommand;
import io.github.hossensyedriadh.cbs.transaction.api.command.CreateWithdrawalCommand;
import io.github.hossensyedriadh.cbs.transaction.api.model.TransactionView;
import io.github.hossensyedriadh.cbs.transaction.api.service.TransactionService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Log4j2
@Service
public class MandatePoster {
    static final String COLLECTION_FEE_CODE = "MANDATE_COLLECTION_FEE";

    private final PaymentMandateRepository paymentMandateRepository;
    private final MandateExecutionRepository mandateExecutionRepository;
    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;
    private final TransactionService transactionService;
    private final ScheduleCalculator scheduleCalculator;
    private final BankHolidayCalendar bankHolidayCalendar;
    private final MandateHistoryWriter mandateHistoryWriter;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public MandatePoster(PaymentMandateRepository paymentMandateRepository, MandateExecutionRepository mandateExecutionRepository,
                         AccountService accountService, AccountBalanceService accountBalanceService, TransactionService transactionService,
                         ScheduleCalculator scheduleCalculator, BankHolidayCalendar bankHolidayCalendar,
                         MandateHistoryWriter mandateHistoryWriter, ApplicationEventPublisher applicationEventPublisher) {
        this.paymentMandateRepository = paymentMandateRepository;
        this.mandateExecutionRepository = mandateExecutionRepository;
        this.accountService = accountService;
        this.accountBalanceService = accountBalanceService;
        this.transactionService = transactionService;
        this.scheduleCalculator = scheduleCalculator;
        this.bankHolidayCalendar = bankHolidayCalendar;
        this.mandateHistoryWriter = mandateHistoryWriter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CollectionOutcome post(Long mandateId, LocalDate businessDate) {
        PaymentMandate mandate = this.paymentMandateRepository.findByIdForUpdate(mandateId).orElse(null);
        if (mandate == null) {
            return CollectionOutcome.IDLE;
        }

        if (mandate.getStatus() == MandateStatus.PENDING) {
            if (mandate.getStartDate().isAfter(businessDate)) {
                return CollectionOutcome.IDLE;
            }
            this.changeStatus(mandate, MandateStatus.ACTIVE, "Start date reached");
        }

        if (!MandateTransitions.canCollect(mandate.getStatus())) {
            return this.finish(mandate, CollectionOutcome.IDLE);
        }

        if (mandate.getScheduleAnchor() == null) {
            mandate.setScheduleAnchor(this.scheduleCalculator.firstAnchor(mandate.getStartDate(), mandate.getFrequency(),
                    mandate.getDayOfMonth(), mandate.getDayOfWeek()));
            this.applyShift(mandate);
        }

        if (mandate.getEndDate() != null && mandate.getScheduleAnchor().isAfter(mandate.getEndDate())) {
            this.changeStatus(mandate, MandateStatus.EXPIRED, "End date passed before the next collection");
            mandate.setNextCollectionDate(null);
            return this.finish(mandate, CollectionOutcome.IDLE);
        }

        Optional<LocalDate> shifted = this.bankHolidayCalendar.shift(mandate.getScheduleAnchor(), mandate.getHolidayAdjustment());
        if (shifted.isEmpty()) {
            return this.skipAndAdvance(mandate, businessDate, ExecutionSkipReason.HOLIDAY, null, "Collection date is not a business day");
        }
        if (shifted.get().isAfter(businessDate)) {
            mandate.setNextCollectionDate(shifted.get());
            return this.finish(mandate, CollectionOutcome.IDLE);
        }
        mandate.setNextCollectionDate(shifted.get());

        MandateExecution existing = this.mandateExecutionRepository
                .findByMandateIdAndCollectionDate(mandate.getId(), mandate.getScheduleAnchor())
                .orElse(null);
        if (existing != null && existing.getStatus() == ExecutionStatus.POSTED) {
            boolean stillDue = this.advance(mandate, businessDate);
            return this.finish(mandate, stillDue ? CollectionOutcome.ADVANCED : CollectionOutcome.IDLE);
        }
        if (existing != null && businessDate.equals(existing.getLastAttemptOn())) {
            return this.finish(mandate, CollectionOutcome.IDLE);
        }

        AccountBalanceView balance = this.accountBalanceService.getBalance(mandate.getDebtorAccountId());
        BigDecimal available = balance.getAvailableBalance() == null ? BigDecimal.ZERO : balance.getAvailableBalance();
        LocalDate monthStart = businessDate.withDayOfMonth(1);
        BigDecimal postedThisMonth = this.mandateExecutionRepository.sumAmount(mandate.getId(), ExecutionStatus.POSTED, monthStart, monthStart.plusMonths(1));
        AmountDecision decision = MandateAmounts.decide(mandate.getAmountType(), mandate.getAmount(), available, mandate.getMinAmount(),
                mandate.getMaxAmount(), postedThisMonth, mandate.getMonthlyCap(), mandate.getCapBehaviour());
        if (decision.skipped()) {
            return this.skipAndAdvance(mandate, businessDate, decision.skipReason(), null, decision.skipReason().name());
        }

        AccountView debtor = this.accountService.getAccount(mandate.getDebtorAccountId()).orElse(null);
        if (debtor == null || debtor.getStatus() != AccountStatus.ACTIVE) {
            return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.ACCOUNT_NOT_ACTIVE, "Debtor account is not active", false);
        }
        if (!mandate.getCurrencyId().equals(debtor.getCurrencyId())) {
            return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.CURRENCY_MISMATCH, "Debtor currency does not match the mandate", true);
        }

        if (mandate.getCreditorScheme() == CreditorScheme.INTERNAL_ACCOUNT) {
            AccountView creditor = this.accountService.getAccount(mandate.getCreditorAccountId()).orElse(null);
            if (creditor == null || creditor.getStatus() == AccountStatus.CLOSED) {
                return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.CREDITOR_UNAVAILABLE, "Creditor account is not available", true);
            }
            if (creditor.getStatus() != AccountStatus.ACTIVE) {
                return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.CREDITOR_UNAVAILABLE, "Creditor account is not active", false);
            }
            if (!mandate.getCurrencyId().equals(creditor.getCurrencyId())) {
                return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.CURRENCY_MISMATCH, "Creditor currency does not match the mandate", true);
            }
        }

        BigDecimal fee = mandate.getCollectionFee() == null ? BigDecimal.ZERO : mandate.getCollectionFee();
        if (available.compareTo(decision.amount().add(fee)) < 0) {
            return this.fail(mandate, existing, businessDate, decision.amount(), ExecutionSkipReason.INSUFFICIENT_FUNDS, "Available balance does not cover the collection", false);
        }

        String description = MandateTexts.paymentDescription(mandate);
        TransactionView payment;
        String idempotencyKey;
        if (mandate.getCreditorScheme() == CreditorScheme.INTERNAL_ACCOUNT) {
            idempotencyKey = MandateIdempotency.transferKey(mandate.getId(), mandate.getScheduleAnchor());
            payment = this.transactionService.transfer(this.transfer(mandate, decision.amount(), idempotencyKey, description));
        } else {
            idempotencyKey = MandateIdempotency.withdrawalKey(mandate.getId(), mandate.getScheduleAnchor());
            payment = this.transactionService.withdraw(this.withdrawal(mandate, decision.amount(), idempotencyKey, description));
        }

        Long feeTransactionId = null;
        if (fee.signum() > 0) {
            feeTransactionId = this.transactionService.charge(this.fee(mandate, fee)).getId();
        }

        MandateExecution execution = existing == null ? this.newExecution(mandate) : existing;
        execution.setAmount(decision.amount());
        execution.setFeeAmount(fee.signum() > 0 ? fee : null);
        execution.setStatus(ExecutionStatus.POSTED);
        execution.setSkipReason(null);
        execution.setTransactionId(payment.getId());
        execution.setTransactionReference(payment.getTransactionReference());
        execution.setFeeTransactionId(feeTransactionId);
        execution.setIdempotencyKey(idempotencyKey);
        execution.setFailureDetail(null);
        execution.setScheduledDate(mandate.getNextCollectionDate());
        this.stamp(execution, businessDate);
        MandateExecution savedExecution = this.mandateExecutionRepository.save(execution);

        mandate.setFailureCount(0);
        this.advance(mandate, businessDate);
        this.paymentMandateRepository.save(mandate);
        this.applicationEventPublisher.publishEvent(new MandateExecutionPostedEvent(mandate.getId(), savedExecution.getId(), decision.amount().toPlainString()));
        log.info("Posted mandate {} collection {} for {}", mandate.getReference(), savedExecution.getId(), decision.amount().toPlainString());
        return CollectionOutcome.POSTED;
    }

    private CollectionOutcome skipAndAdvance(PaymentMandate mandate, LocalDate businessDate, ExecutionSkipReason reason,
                                             BigDecimal amount, String detail) {
        MandateExecution execution = this.mandateExecutionRepository
                .findByMandateIdAndCollectionDate(mandate.getId(), mandate.getScheduleAnchor())
                .orElseGet(() -> this.newExecution(mandate));
        if (execution.getStatus() == ExecutionStatus.POSTED) {
            boolean stillDue = this.advance(mandate, businessDate);
            return this.finish(mandate, stillDue ? CollectionOutcome.ADVANCED : CollectionOutcome.IDLE);
        }

        execution.setAmount(amount);
        execution.setStatus(ExecutionStatus.SKIPPED);
        execution.setSkipReason(reason);
        execution.setFailureDetail(MandateTexts.clip(detail, 500));
        execution.setScheduledDate(mandate.getNextCollectionDate());
        this.stamp(execution, businessDate);
        this.mandateExecutionRepository.save(execution);
        this.advance(mandate, businessDate);
        log.info("Skipped mandate {} on {} because {}", mandate.getReference(), mandate.getScheduleAnchor(), reason);
        return this.finish(mandate, CollectionOutcome.SKIPPED);
    }

    private CollectionOutcome fail(PaymentMandate mandate, MandateExecution existing, LocalDate businessDate, BigDecimal amount,
                                   ExecutionSkipReason reason, String detail, boolean permanent) {
        MandateExecution execution = existing == null ? this.newExecution(mandate) : existing;
        execution.setAmount(amount);
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setSkipReason(reason);
        execution.setFailureDetail(MandateTexts.clip(detail, 500));
        execution.setScheduledDate(mandate.getNextCollectionDate());
        this.stamp(execution, businessDate);
        MandateExecution saved = this.mandateExecutionRepository.save(execution);

        if (permanent) {
            this.suspend(mandate, detail);
        } else {
            int failures = (mandate.getFailureCount() == null ? 0 : mandate.getFailureCount()) + 1;
            mandate.setFailureCount(failures);
            int maxRetries = mandate.getMaxRetries() == null ? 3 : mandate.getMaxRetries();
            if (failures >= maxRetries) {
                this.suspend(mandate, "Retry limit reached: " + reason.name());
            }
        }

        this.paymentMandateRepository.save(mandate);
        this.applicationEventPublisher.publishEvent(new MandateExecutionFailedEvent(mandate.getId(), saved.getId(), reason.name()));
        log.warn("Mandate {} collection failed: {}", mandate.getReference(), detail);
        return CollectionOutcome.FAILED;
    }

    private boolean advance(PaymentMandate mandate, LocalDate businessDate) {
        LocalDate nextAnchor = this.scheduleCalculator.advance(mandate.getScheduleAnchor(), mandate.getFrequency(), mandate.getDayOfMonth());
        if (mandate.getEndDate() != null && nextAnchor.isAfter(mandate.getEndDate())) {
            this.changeStatus(mandate, MandateStatus.COMPLETED, "Final collection date has passed");
            mandate.setCompletedAt(OffsetDateTime.now());
            mandate.setNextCollectionDate(null);
            return false;
        }

        mandate.setScheduleAnchor(nextAnchor);
        Optional<LocalDate> shifted = this.bankHolidayCalendar.shift(nextAnchor, mandate.getHolidayAdjustment());
        mandate.setNextCollectionDate(shifted.orElse(nextAnchor));
        return !mandate.getNextCollectionDate().isAfter(businessDate) && mandate.getStatus() == MandateStatus.ACTIVE;
    }

    private void suspend(PaymentMandate mandate, String reason) {
        if (!MandateTransitions.canSuspend(mandate.getStatus())) {
            return;
        }
        this.changeStatus(mandate, MandateStatus.SUSPENDED, reason);
        mandate.setSuspendedAt(OffsetDateTime.now());
    }

    private void changeStatus(PaymentMandate mandate, MandateStatus to, String reason) {
        MandateStatus from = mandate.getStatus();
        if (from == to) {
            return;
        }
        mandate.setStatus(to);
        this.mandateHistoryWriter.status(mandate.getId(), from, to, reason);
        this.applicationEventPublisher.publishEvent(new MandateStatusChangedEvent(mandate.getId(), from.name(), to.name(), MandateTexts.clip(reason, 500)));
    }

    private void applyShift(PaymentMandate mandate) {
        this.bankHolidayCalendar.shift(mandate.getScheduleAnchor(), mandate.getHolidayAdjustment())
                .ifPresentOrElse(mandate::setNextCollectionDate, () -> mandate.setNextCollectionDate(mandate.getScheduleAnchor()));
    }

    private CollectionOutcome finish(PaymentMandate mandate, CollectionOutcome outcome) {
        this.paymentMandateRepository.save(mandate);
        return outcome;
    }

    private MandateExecution newExecution(PaymentMandate mandate) {
        MandateExecution created = new MandateExecution();
        created.setMandateId(mandate.getId());
        created.setCollectionDate(mandate.getScheduleAnchor());
        created.setAttemptCount(0);
        return created;
    }

    private void stamp(MandateExecution execution, LocalDate businessDate) {
        int attempts = execution.getAttemptCount() == null ? 0 : execution.getAttemptCount();
        execution.setAttemptCount(attempts + 1);
        execution.setLastAttemptOn(businessDate);
        execution.setAttemptedAt(OffsetDateTime.now());
    }

    private CreateTransferCommand transfer(PaymentMandate mandate, BigDecimal amount, String idempotencyKey, String description) {
        CreateTransferCommand command = new CreateTransferCommand();
        command.setAccountId(mandate.getDebtorAccountId());
        command.setDestinationAccountId(mandate.getCreditorAccountId());
        command.setCurrencyId(mandate.getCurrencyId());
        command.setAmount(amount);
        command.setIdempotencyKey(idempotencyKey);
        command.setDescription(description);
        return command;
    }

    private CreateWithdrawalCommand withdrawal(PaymentMandate mandate, BigDecimal amount, String idempotencyKey, String description) {
        CreateWithdrawalCommand command = new CreateWithdrawalCommand();
        command.setAccountId(mandate.getDebtorAccountId());
        command.setCurrencyId(mandate.getCurrencyId());
        command.setAmount(amount);
        command.setIdempotencyKey(idempotencyKey);
        command.setDescription(description);
        return command;
    }

    private ChargeFeeCommand fee(PaymentMandate mandate, BigDecimal amount) {
        ChargeFeeCommand command = new ChargeFeeCommand();
        command.setAccountId(mandate.getDebtorAccountId());
        command.setCurrencyId(mandate.getCurrencyId());
        command.setAmount(amount);
        command.setFeeCode(COLLECTION_FEE_CODE);
        command.setIdempotencyKey(MandateIdempotency.feeKey(mandate.getId(), mandate.getScheduleAnchor()));
        command.setDescription(MandateTexts.clip("Collection fee " + mandate.getReference(), 255));
        return command;
    }
}
