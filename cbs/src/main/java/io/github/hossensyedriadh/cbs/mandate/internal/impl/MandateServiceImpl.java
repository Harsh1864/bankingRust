package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.account.api.enumerator.AccountStatus;
import io.github.hossensyedriadh.cbs.account.api.model.AccountHolderView;
import io.github.hossensyedriadh.cbs.account.api.model.AccountView;
import io.github.hossensyedriadh.cbs.account.api.service.AccountService;
import io.github.hossensyedriadh.cbs.eod.api.service.BusinessDateService;
import io.github.hossensyedriadh.cbs.exception.ApiResourceException;
import io.github.hossensyedriadh.cbs.mandate.api.command.AmendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.CancelMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.OpenMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.ResumeMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.SuspendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmendmentKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateAmendedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateOpenedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateStatusChangedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateAmendmentView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateSchedulePreview;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateStatusHistoryView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateView;
import io.github.hossensyedriadh.cbs.mandate.api.model.ScheduledOccurrence;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateService;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateAmendment;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import io.github.hossensyedriadh.cbs.mandate.internal.mapper.MandateMapper;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateAmendmentRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateStatusHistoryRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.PaymentMandateRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.util.BankHolidayCalendar;
import io.github.hossensyedriadh.cbs.mandate.internal.util.IbanCheck;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateAmounts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateReferenceGenerator;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTexts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTransitions;
import io.github.hossensyedriadh.cbs.mandate.internal.util.ScheduleCalculator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
@Validated
@Transactional(readOnly = true)
public class MandateServiceImpl implements MandateService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int DEFAULT_RETRIES = 3;

    private final PaymentMandateRepository paymentMandateRepository;
    private final MandateAmendmentRepository mandateAmendmentRepository;
    private final MandateStatusHistoryRepository mandateStatusHistoryRepository;
    private final AccountService accountService;
    private final BusinessDateService businessDateService;
    private final ScheduleCalculator scheduleCalculator;
    private final BankHolidayCalendar bankHolidayCalendar;
    private final MandateReferenceGenerator mandateReferenceGenerator;
    private final MandateMapper mandateMapper;
    private final MandateHistoryWriter mandateHistoryWriter;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HttpServletRequest httpServletRequest;

    @Autowired
    public MandateServiceImpl(PaymentMandateRepository paymentMandateRepository, MandateAmendmentRepository mandateAmendmentRepository,
                              MandateStatusHistoryRepository mandateStatusHistoryRepository, AccountService accountService,
                              BusinessDateService businessDateService, ScheduleCalculator scheduleCalculator,
                              BankHolidayCalendar bankHolidayCalendar, MandateReferenceGenerator mandateReferenceGenerator,
                              MandateMapper mandateMapper, MandateHistoryWriter mandateHistoryWriter,
                              ApplicationEventPublisher applicationEventPublisher, HttpServletRequest httpServletRequest) {
        this.paymentMandateRepository = paymentMandateRepository;
        this.mandateAmendmentRepository = mandateAmendmentRepository;
        this.mandateStatusHistoryRepository = mandateStatusHistoryRepository;
        this.accountService = accountService;
        this.businessDateService = businessDateService;
        this.scheduleCalculator = scheduleCalculator;
        this.bankHolidayCalendar = bankHolidayCalendar;
        this.mandateReferenceGenerator = mandateReferenceGenerator;
        this.mandateMapper = mandateMapper;
        this.mandateHistoryWriter = mandateHistoryWriter;
        this.applicationEventPublisher = applicationEventPublisher;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public Optional<MandateView> getMandate(Long id) {
        return this.paymentMandateRepository.findById(id).map(this.mandateMapper::toView);
    }

    @Override
    public List<MandateView> getMandatesForDebtor(Long accountId) {
        this.requireAccount(accountId);
        return this.paymentMandateRepository.findAllByDebtorAccountId(accountId).stream().map(this.mandateMapper::toView).toList();
    }

    @Override
    public List<MandateView> getMandatesForCreditor(Long accountId) {
        this.requireAccount(accountId);
        return this.paymentMandateRepository.findAllByCreditorAccountId(accountId).stream().map(this.mandateMapper::toView).toList();
    }

    @Override
    public List<MandateAmendmentView> getAmendments(Long mandateId) {
        this.requireMandate(mandateId);
        return this.mandateAmendmentRepository.findAllByMandateIdOrderByAmendedAtDesc(mandateId).stream()
                .map(this.mandateMapper::toAmendmentView)
                .toList();
    }

    @Override
    public List<MandateStatusHistoryView> getStatusHistory(Long mandateId) {
        this.requireMandate(mandateId);
        return this.mandateStatusHistoryRepository.findAllByMandateIdOrderByChangedAtDesc(mandateId).stream()
                .map(this.mandateMapper::toHistoryView)
                .toList();
    }

    @Override
    public MandateSchedulePreview preview(Long mandateId, int occurrences) {
        PaymentMandate mandate = this.requireMandate(mandateId);
        int count = Math.max(1, Math.min(occurrences, 36));
        LocalDate anchor = mandate.getScheduleAnchor() == null ? mandate.getStartDate() : mandate.getScheduleAnchor();
        List<LocalDate> contractualDates = this.scheduleCalculator.preview(anchor, mandate.getEndDate(), mandate.getFrequency(),
                mandate.getDayOfMonth(), count);

        List<ScheduledOccurrence> occurrencesView = new ArrayList<>();
        for (LocalDate contractualDate : contractualDates) {
            ScheduledOccurrence occurrence = new ScheduledOccurrence();
            occurrence.setContractualDate(contractualDate);
            this.bankHolidayCalendar.shift(contractualDate, mandate.getHolidayAdjustment()).ifPresentOrElse(shifted -> {
                occurrence.setCollectionDate(shifted);
                occurrence.setSkippedHoliday(false);
            }, () -> {
                occurrence.setCollectionDate(null);
                occurrence.setSkippedHoliday(true);
            });
            occurrencesView.add(occurrence);
        }

        MandateSchedulePreview preview = new MandateSchedulePreview();
        preview.setMandateId(mandate.getId());
        preview.setOccurrences(occurrencesView);
        return preview;
    }

    @Transactional
    @Override
    public MandateView open(@Valid OpenMandateCommand command) {
        AccountView debtor = this.requireActiveAccount(command.getDebtorAccountId());
        if (!debtor.getCurrencyId().equals(command.getCurrencyId())) {
            throw this.badRequest("Mandate currency does not match the debtor account");
        }

        AccountView creditor = null;
        if (command.getCreditorScheme() == CreditorScheme.INTERNAL_ACCOUNT) {
            creditor = this.requireActiveAccount(command.getCreditorAccountId());
            if (debtor.getId().equals(creditor.getId())) {
                throw this.badRequest("Debtor and creditor accounts must be different");
            }
            if (!debtor.getCurrencyId().equals(creditor.getCurrencyId())) {
                throw this.badRequest("Debtor and creditor accounts must use the same currency");
            }
        } else if (!IbanCheck.isValid(command.getExternalCreditorIban())) {
            throw this.badRequest("A valid IBAN is required");
        }

        if (command.getKind() == MandateKind.SWEEP) {
            if (creditor == null || !this.sharesCustomer(debtor, creditor)) {
                throw this.badRequest("A sweep must move funds between accounts of the same customer");
            }
        }

        LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
        if (command.getEndDate() != null && command.getEndDate().isBefore(businessDate)) {
            throw this.badRequest("End date is already past");
        }

        LocalDate anchor = this.scheduleCalculator.firstAnchor(command.getStartDate(), command.getFrequency(),
                command.getDayOfMonth(), command.getDayOfWeek());

        PaymentMandate mandate = new PaymentMandate();
        mandate.setReference("MD-" + this.mandateReferenceGenerator.generate());
        mandate.setDebtorAccountId(debtor.getId());
        mandate.setCreditorAccountId(creditor == null ? null : creditor.getId());
        mandate.setCurrencyId(command.getCurrencyId());
        mandate.setKind(command.getKind());
        mandate.setCreditorScheme(command.getCreditorScheme());
        mandate.setFrequency(command.getFrequency());
        mandate.setAmountType(command.getAmountType());
        mandate.setAmount(MandateAmounts.money(command.getAmount()));
        mandate.setDayOfMonth(command.getDayOfMonth());
        mandate.setDayOfWeek(command.getDayOfWeek());
        mandate.setStartDate(command.getStartDate());
        mandate.setEndDate(command.getEndDate());
        mandate.setScheduleAnchor(anchor);
        mandate.setHolidayAdjustment(command.getHolidayAdjustment());
        mandate.setCapBehaviour(command.getCapBehaviour());
        mandate.setMinAmount(this.scaleOrNull(command.getMinAmount()));
        mandate.setMaxAmount(this.scaleOrNull(command.getMaxAmount()));
        mandate.setMonthlyCap(this.scaleOrNull(command.getMonthlyCap()));
        mandate.setCollectionFee(this.scaleOrNull(command.getCollectionFee()));
        mandate.setFailureCount(0);
        mandate.setMaxRetries(command.getMaxRetries() == null ? DEFAULT_RETRIES : command.getMaxRetries());
        mandate.setExternalCreditorName(MandateTexts.clip(command.getExternalCreditorName(), 140));
        mandate.setExternalCreditorIban(command.getExternalCreditorIban() == null ? null : command.getExternalCreditorIban().replace(" ", "").toUpperCase());
        mandate.setNarrative(MandateTexts.clip(command.getNarrative(), 255));
        mandate.setOpenedAt(OffsetDateTime.now());
        mandate.setStatus(command.getStartDate().isAfter(businessDate) ? MandateStatus.PENDING : MandateStatus.ACTIVE);
        this.applyNextCollection(mandate);

        PaymentMandate saved = this.paymentMandateRepository.save(mandate);
        this.mandateHistoryWriter.status(saved.getId(), null, saved.getStatus(), "Mandate opened");
        this.applicationEventPublisher.publishEvent(new MandateOpenedEvent(saved.getId(), saved.getReference(), saved.getKind().name()));
        this.applicationEventPublisher.publishEvent(new MandateStatusChangedEvent(saved.getId(), null, saved.getStatus().name(), "Mandate opened"));

        log.info("Opened mandate {} for debtor account {}", saved.getReference(), saved.getDebtorAccountId());
        return this.mandateMapper.toView(saved);
    }

    @Transactional
    @Override
    public MandateView amend(@Valid AmendMandateCommand command) {
        PaymentMandate mandate = this.paymentMandateRepository.findByIdForUpdate(command.getMandateId())
                .orElseThrow(() -> this.notFound("Mandate not found"));

        if (!MandateTransitions.canAmend(mandate.getStatus())) {
            throw this.conflict("Mandate cannot be amended from status " + mandate.getStatus());
        }

        if (Boolean.TRUE.equals(command.getClearEndDate()) && command.getEndDate() != null) {
            throw this.badRequest("Send either a new end date or a request to clear it");
        }
        this.rejectConflictingClear(command.getClearMinAmount(), command.getMinAmount(), "minimum amount");
        this.rejectConflictingClear(command.getClearMaxAmount(), command.getMaxAmount(), "maximum amount");
        this.rejectConflictingClear(command.getClearMonthlyCap(), command.getMonthlyCap(), "monthly cap");
        this.rejectConflictingClear(command.getClearCollectionFee(), command.getCollectionFee(), "collection fee");

        if (command.getCreditorAccountId() != null && (command.getExternalCreditorIban() != null || command.getExternalCreditorName() != null)) {
            throw this.badRequest("Choose an internal creditor account or an external IBAN");
        }

        List<MandateAmendment> amendments = new ArrayList<>();
        String reason = command.getReason().trim();
        boolean scheduleChanged = false;

        if (command.getAmount() != null) {
            this.assertAmountForType(mandate.getAmountType(), command.getAmount());
            this.record(amendments, mandate.getId(), AmendmentKind.AMOUNT, this.moneyText(mandate.getAmount()), this.moneyText(command.getAmount()), reason);
            mandate.setAmount(MandateAmounts.money(command.getAmount()));
        }

        if (command.getFrequency() != null && command.getFrequency() != mandate.getFrequency()) {
            this.record(amendments, mandate.getId(), AmendmentKind.FREQUENCY, mandate.getFrequency(), command.getFrequency(), reason);
            mandate.setFrequency(command.getFrequency());
            scheduleChanged = true;
        }

        if (command.getDayOfMonth() != null && !command.getDayOfMonth().equals(mandate.getDayOfMonth())) {
            this.record(amendments, mandate.getId(), AmendmentKind.DAY_OF_MONTH, mandate.getDayOfMonth(), command.getDayOfMonth(), reason);
            mandate.setDayOfMonth(command.getDayOfMonth());
            scheduleChanged = true;
        }

        if (command.getDayOfWeek() != null && !command.getDayOfWeek().equals(mandate.getDayOfWeek())) {
            this.record(amendments, mandate.getId(), AmendmentKind.DAY_OF_WEEK, mandate.getDayOfWeek(), command.getDayOfWeek(), reason);
            mandate.setDayOfWeek(command.getDayOfWeek());
            scheduleChanged = true;
        }

        if (Boolean.TRUE.equals(command.getClearEndDate()) && mandate.getEndDate() != null) {
            this.record(amendments, mandate.getId(), AmendmentKind.END_DATE, mandate.getEndDate(), null, reason);
            mandate.setEndDate(null);
        } else if (command.getEndDate() != null && !command.getEndDate().equals(mandate.getEndDate())) {
            if (command.getEndDate().isBefore(mandate.getStartDate())) {
                throw this.badRequest("End date cannot be before the start date");
            }
            LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
            if (command.getEndDate().isBefore(businessDate)) {
                throw this.badRequest("End date is already past");
            }
            this.record(amendments, mandate.getId(), AmendmentKind.END_DATE, mandate.getEndDate(), command.getEndDate(), reason);
            mandate.setEndDate(command.getEndDate());
        }

        if (command.getHolidayAdjustment() != null && command.getHolidayAdjustment() != mandate.getHolidayAdjustment()) {
            this.record(amendments, mandate.getId(), AmendmentKind.HOLIDAY_ADJUSTMENT, mandate.getHolidayAdjustment(), command.getHolidayAdjustment(), reason);
            mandate.setHolidayAdjustment(command.getHolidayAdjustment());
            scheduleChanged = true;
        }

        if (command.getCapBehaviour() != null && command.getCapBehaviour() != mandate.getCapBehaviour()) {
            this.record(amendments, mandate.getId(), AmendmentKind.CAP_BEHAVIOUR, mandate.getCapBehaviour(), command.getCapBehaviour(), reason);
            mandate.setCapBehaviour(command.getCapBehaviour());
        }

        this.applyOptionalAmount(amendments, mandate, AmendmentKind.MIN_AMOUNT, command.getMinAmount(), command.getClearMinAmount(), reason);
        this.applyOptionalAmount(amendments, mandate, AmendmentKind.MAX_AMOUNT, command.getMaxAmount(), command.getClearMaxAmount(), reason);
        this.applyOptionalAmount(amendments, mandate, AmendmentKind.MONTHLY_CAP, command.getMonthlyCap(), command.getClearMonthlyCap(), reason);
        this.applyOptionalAmount(amendments, mandate, AmendmentKind.COLLECTION_FEE, command.getCollectionFee(), command.getClearCollectionFee(), reason);

        if (mandate.getMinAmount() != null && mandate.getMaxAmount() != null && mandate.getMinAmount().compareTo(mandate.getMaxAmount()) > 0) {
            throw this.badRequest("Minimum amount cannot exceed the maximum amount");
        }
        if (mandate.getMonthlyCap() != null && mandate.getMaxAmount() != null && mandate.getMonthlyCap().compareTo(mandate.getMaxAmount()) < 0) {
            throw this.badRequest("Monthly cap cannot be below the per-collection maximum");
        }

        if (command.getMaxRetries() != null && !command.getMaxRetries().equals(mandate.getMaxRetries())) {
            this.record(amendments, mandate.getId(), AmendmentKind.MAX_RETRIES, mandate.getMaxRetries(), command.getMaxRetries(), reason);
            mandate.setMaxRetries(command.getMaxRetries());
        }

        if (command.getCreditorAccountId() != null) {
            this.moveToInternalCreditor(mandate, command.getCreditorAccountId(), amendments, reason);
        } else if (command.getExternalCreditorIban() != null || command.getExternalCreditorName() != null) {
            this.moveToExternalCreditor(mandate, command.getExternalCreditorName(), command.getExternalCreditorIban(), amendments, reason);
        }

        if (command.getNarrative() != null) {
            String narrative = MandateTexts.clip(command.getNarrative(), 255);
            this.record(amendments, mandate.getId(), AmendmentKind.NARRATIVE, mandate.getNarrative(), narrative, reason);
            mandate.setNarrative(narrative);
        }

        this.requireScheduleFields(mandate);

        if (scheduleChanged) {
            LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
            LocalDate basis = businessDate.isAfter(mandate.getStartDate()) ? businessDate : mandate.getStartDate();
            LocalDate anchor = this.scheduleCalculator.firstAnchor(basis, mandate.getFrequency(), mandate.getDayOfMonth(), mandate.getDayOfWeek());
            this.record(amendments, mandate.getId(), AmendmentKind.SCHEDULE, mandate.getScheduleAnchor(), anchor, reason);
            mandate.setScheduleAnchor(anchor);
            this.applyNextCollection(mandate);
        }

        if (amendments.isEmpty()) {
            throw this.badRequest("No mandate changes were supplied");
        }

        this.mandateHistoryWriter.amendments(amendments);
        PaymentMandate saved = this.paymentMandateRepository.save(mandate);
        String amendmentKind = amendments.size() == 1 ? amendments.get(0).getKind().name() : "MULTIPLE";
        this.applicationEventPublisher.publishEvent(new MandateAmendedEvent(saved.getId(), amendmentKind));
        log.info("Amended mandate {} with {} change(s)", saved.getReference(), amendments.size());
        return this.mandateMapper.toView(saved);
    }

    @Transactional
    @Override
    public void suspend(@Valid SuspendMandateCommand command) {
        PaymentMandate mandate = this.lockedMandate(command.getMandateId());
        if (!MandateTransitions.canSuspend(mandate.getStatus())) {
            throw this.conflict("Mandate cannot be suspended from status " + mandate.getStatus());
        }
        this.changeStatus(mandate, MandateStatus.SUSPENDED, command.getReason());
        mandate.setSuspendedAt(OffsetDateTime.now());
        this.paymentMandateRepository.save(mandate);
    }

    @Transactional
    @Override
    public void resume(@Valid ResumeMandateCommand command) {
        PaymentMandate mandate = this.lockedMandate(command.getMandateId());
        if (!MandateTransitions.canResume(mandate.getStatus())) {
            throw this.conflict("Mandate cannot be resumed from status " + mandate.getStatus());
        }

        LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
        if (mandate.getEndDate() != null && mandate.getEndDate().isBefore(businessDate)) {
            throw this.badRequest("Mandate end date has passed");
        }

        this.changeStatus(mandate, MandateStatus.ACTIVE, command.getReason());
        mandate.setSuspendedAt(null);
        mandate.setFailureCount(0);
        if (mandate.getScheduleAnchor() == null || mandate.getNextCollectionDate() == null
                || mandate.getNextCollectionDate().isBefore(businessDate)) {
            LocalDate basis = businessDate.isAfter(mandate.getStartDate()) ? businessDate : mandate.getStartDate();
            mandate.setScheduleAnchor(this.scheduleCalculator.firstAnchor(basis, mandate.getFrequency(), mandate.getDayOfMonth(), mandate.getDayOfWeek()));
            this.applyNextCollection(mandate);
        }
        this.paymentMandateRepository.save(mandate);
    }

    @Transactional
    @Override
    public void cancel(@Valid CancelMandateCommand command) {
        PaymentMandate mandate = this.lockedMandate(command.getMandateId());
        if (!MandateTransitions.canCancel(mandate.getStatus())) {
            throw this.conflict("Mandate cannot be cancelled from status " + mandate.getStatus());
        }
        this.changeStatus(mandate, MandateStatus.CANCELLED, command.getReason());
        mandate.setCancelledAt(OffsetDateTime.now());
        mandate.setNextCollectionDate(null);
        this.paymentMandateRepository.save(mandate);
    }

    private void moveToInternalCreditor(PaymentMandate mandate, Long creditorAccountId, List<MandateAmendment> amendments, String reason) {
        if (mandate.getKind() == MandateKind.SWEEP || mandate.getCreditorScheme() == CreditorScheme.INTERNAL_ACCOUNT) {
            AccountView debtor = this.requireActiveAccount(mandate.getDebtorAccountId());
            AccountView creditor = this.requireActiveAccount(creditorAccountId);
            if (debtor.getId().equals(creditor.getId())) {
                throw this.badRequest("Debtor and creditor accounts must be different");
            }
            if (!debtor.getCurrencyId().equals(creditor.getCurrencyId()) || !mandate.getCurrencyId().equals(creditor.getCurrencyId())) {
                throw this.badRequest("Creditor account currency does not match the mandate");
            }
            if (mandate.getKind() == MandateKind.SWEEP && !this.sharesCustomer(debtor, creditor)) {
                throw this.badRequest("A sweep must move funds between accounts of the same customer");
            }
        }

        AccountView creditor = this.requireActiveAccount(creditorAccountId);
        if (!mandate.getCurrencyId().equals(creditor.getCurrencyId())) {
            throw this.badRequest("Creditor account currency does not match the mandate");
        }
        if (mandate.getDebtorAccountId().equals(creditor.getId())) {
            throw this.badRequest("Debtor and creditor accounts must be different");
        }

        this.record(amendments, mandate.getId(), AmendmentKind.CREDITOR, mandate.getCreditorAccountId(), creditor.getId(), reason);
        if (mandate.getCreditorScheme() != CreditorScheme.INTERNAL_ACCOUNT) {
            this.record(amendments, mandate.getId(), AmendmentKind.EXTERNAL_CREDITOR, mandate.getCreditorScheme(), CreditorScheme.INTERNAL_ACCOUNT, reason);
        }
        mandate.setCreditorAccountId(creditor.getId());
        mandate.setCreditorScheme(CreditorScheme.INTERNAL_ACCOUNT);
        mandate.setExternalCreditorIban(null);
        mandate.setExternalCreditorName(null);
    }

    private void moveToExternalCreditor(PaymentMandate mandate, String name, String iban, List<MandateAmendment> amendments, String reason) {
        if (mandate.getKind() == MandateKind.SWEEP) {
            throw this.badRequest("A sweep can only pay another account in this bank");
        }
        if (!IbanCheck.isValid(iban)) {
            throw this.badRequest("A valid IBAN is required");
        }
        if (name == null || name.isBlank()) {
            throw this.badRequest("External creditor name is required");
        }

        String compactIban = iban.replace(" ", "").toUpperCase();
        this.record(amendments, mandate.getId(), AmendmentKind.EXTERNAL_CREDITOR, mandate.getExternalCreditorIban(), compactIban, reason);
        mandate.setExternalCreditorIban(compactIban);
        mandate.setExternalCreditorName(MandateTexts.clip(name, 140));
        mandate.setCreditorScheme(CreditorScheme.EXTERNAL_IBAN);
        mandate.setCreditorAccountId(null);
    }

    private void applyOptionalAmount(List<MandateAmendment> amendments, PaymentMandate mandate, AmendmentKind kind,
                                     BigDecimal supplied, Boolean clear, String reason) {
        BigDecimal current = switch (kind) {
            case MIN_AMOUNT -> mandate.getMinAmount();
            case MAX_AMOUNT -> mandate.getMaxAmount();
            case MONTHLY_CAP -> mandate.getMonthlyCap();
            case COLLECTION_FEE -> mandate.getCollectionFee();
            default -> null;
        };

        if (Boolean.TRUE.equals(clear)) {
            if (current == null) {
                return;
            }
            this.record(amendments, mandate.getId(), kind, this.moneyText(current), null, reason);
            this.writeOptionalAmount(mandate, kind, null);
            return;
        }

        if (supplied == null) {
            return;
        }

        BigDecimal scaled = MandateAmounts.money(supplied);
        this.record(amendments, mandate.getId(), kind, this.moneyText(current), this.moneyText(scaled), reason);
        this.writeOptionalAmount(mandate, kind, scaled);
    }

    private void writeOptionalAmount(PaymentMandate mandate, AmendmentKind kind, BigDecimal value) {
        switch (kind) {
            case MIN_AMOUNT -> mandate.setMinAmount(value);
            case MAX_AMOUNT -> mandate.setMaxAmount(value);
            case MONTHLY_CAP -> mandate.setMonthlyCap(value);
            case COLLECTION_FEE -> mandate.setCollectionFee(value);
            default -> {
            }
        }
    }

    private void requireScheduleFields(PaymentMandate mandate) {
        if (this.needsDayOfMonth(mandate.getFrequency()) && mandate.getDayOfMonth() == null) {
            throw this.badRequest("Day of month is required for this frequency");
        }
        if (this.needsDayOfWeek(mandate.getFrequency()) && mandate.getDayOfWeek() == null) {
            throw this.badRequest("Day of week is required for this frequency");
        }
    }

    private boolean needsDayOfMonth(PaymentFrequency frequency) {
        return frequency == PaymentFrequency.MONTHLY || frequency == PaymentFrequency.QUARTERLY
                || frequency == PaymentFrequency.SEMI_ANNUAL || frequency == PaymentFrequency.ANNUAL;
    }

    private boolean needsDayOfWeek(PaymentFrequency frequency) {
        return frequency == PaymentFrequency.WEEKLY || frequency == PaymentFrequency.FORTNIGHTLY;
    }

    private void assertAmountForType(AmountType amountType, BigDecimal amount) {
        if (amountType == AmountType.PERCENT_OF_BALANCE && amount.compareTo(ONE_HUNDRED) > 0) {
            throw this.badRequest("Percent of balance cannot exceed 100");
        }
    }

    private void changeStatus(PaymentMandate mandate, MandateStatus to, String reason) {
        MandateStatus from = mandate.getStatus();
        mandate.setStatus(to);
        this.mandateHistoryWriter.status(mandate.getId(), from, to, reason);
        this.applicationEventPublisher.publishEvent(new MandateStatusChangedEvent(mandate.getId(), from.name(), to.name(), MandateTexts.clip(reason, 500)));
    }

    private void applyNextCollection(PaymentMandate mandate) {
        this.bankHolidayCalendar.shift(mandate.getScheduleAnchor(), mandate.getHolidayAdjustment())
                .ifPresentOrElse(mandate::setNextCollectionDate, () -> mandate.setNextCollectionDate(mandate.getScheduleAnchor()));
    }

    private PaymentMandate lockedMandate(Long id) {
        return this.paymentMandateRepository.findByIdForUpdate(id).orElseThrow(() -> this.notFound("Mandate not found"));
    }

    private PaymentMandate requireMandate(Long id) {
        return this.paymentMandateRepository.findById(id).orElseThrow(() -> this.notFound("Mandate not found"));
    }

    private AccountView requireAccount(Long accountId) {
        return this.accountService.getAccount(accountId).orElseThrow(() -> {
            log.warn("Account not found with id {}", accountId);
            return this.notFound("Account not found");
        });
    }

    private AccountView requireActiveAccount(Long accountId) {
        AccountView account = this.requireAccount(accountId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw this.badRequest("Account is not active");
        }
        return account;
    }

    private boolean sharesCustomer(AccountView left, AccountView right) {
        if (left.getAccountHolders() == null || right.getAccountHolders() == null) {
            return false;
        }
        Set<Long> customers = new HashSet<>();
        for (AccountHolderView holder : left.getAccountHolders()) {
            if (holder.getCustomerId() != null) {
                customers.add(holder.getCustomerId());
            }
        }
        for (AccountHolderView holder : right.getAccountHolders()) {
            if (holder.getCustomerId() != null && customers.contains(holder.getCustomerId())) {
                return true;
            }
        }
        return false;
    }

    private void record(List<MandateAmendment> rows, Long mandateId, AmendmentKind kind, Object previous, Object next, String reason) {
        String before = MandateTexts.clip(MandateTexts.text(previous), 255);
        String after = MandateTexts.clip(MandateTexts.text(next), 255);
        if (Objects.equals(before, after)) {
            return;
        }
        MandateAmendment amendment = new MandateAmendment();
        amendment.setMandateId(mandateId);
        amendment.setKind(kind);
        amendment.setPreviousValue(before);
        amendment.setNewValue(after);
        amendment.setReason(MandateTexts.clip(reason, 500));
        amendment.setAmendedAt(OffsetDateTime.now());
        rows.add(amendment);
    }

    private void rejectConflictingClear(Boolean clear, BigDecimal supplied, String label) {
        if (Boolean.TRUE.equals(clear) && supplied != null) {
            throw this.badRequest("Send either a new " + label + " or a request to clear it");
        }
    }

    private BigDecimal scaleOrNull(BigDecimal value) {
        return value == null ? null : MandateAmounts.money(value);
    }

    private String moneyText(BigDecimal value) {
        return value == null ? null : MandateAmounts.money(value).toPlainString();
    }

    private ApiResourceException badRequest(String message) {
        return new ApiResourceException(message, HttpStatus.BAD_REQUEST, this.httpServletRequest);
    }

    private ApiResourceException notFound(String message) {
        return new ApiResourceException(message, HttpStatus.NOT_FOUND, this.httpServletRequest);
    }

    private ApiResourceException conflict(String message) {
        return new ApiResourceException(message, HttpStatus.CONFLICT, this.httpServletRequest);
    }
}
