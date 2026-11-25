package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.eod.api.service.BusinessDateService;
import io.github.hossensyedriadh.cbs.exception.ApiResourceException;
import io.github.hossensyedriadh.cbs.mandate.api.command.RecallMandateExecutionCommand;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionRecalledEvent;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateRecallService;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import io.github.hossensyedriadh.cbs.mandate.internal.mapper.MandateMapper;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateExecutionRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.PaymentMandateRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.util.BankHolidayCalendar;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateIdempotency;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateNotice;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateRecallPolicy;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTexts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.RecallDecision;
import io.github.hossensyedriadh.cbs.transaction.api.command.ReverseTransactionCommand;
import io.github.hossensyedriadh.cbs.transaction.api.model.TransactionView;
import io.github.hossensyedriadh.cbs.transaction.api.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

@Log4j2
@Service
@Validated
public class MandateRecallServiceImpl implements MandateRecallService {
    private final MandateExecutionRepository mandateExecutionRepository;
    private final PaymentMandateRepository paymentMandateRepository;
    private final TransactionService transactionService;
    private final BusinessDateService businessDateService;
    private final BankHolidayCalendar bankHolidayCalendar;
    private final MandateMapper mandateMapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HttpServletRequest httpServletRequest;

    @Autowired
    public MandateRecallServiceImpl(MandateExecutionRepository mandateExecutionRepository, PaymentMandateRepository paymentMandateRepository,
                                    TransactionService transactionService, BusinessDateService businessDateService,
                                    BankHolidayCalendar bankHolidayCalendar, MandateMapper mandateMapper,
                                    ApplicationEventPublisher applicationEventPublisher, HttpServletRequest httpServletRequest) {
        this.mandateExecutionRepository = mandateExecutionRepository;
        this.paymentMandateRepository = paymentMandateRepository;
        this.transactionService = transactionService;
        this.businessDateService = businessDateService;
        this.bankHolidayCalendar = bankHolidayCalendar;
        this.mandateMapper = mandateMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.httpServletRequest = httpServletRequest;
    }

    @Transactional
    @Override
    public MandateExecutionView recall(@Valid RecallMandateExecutionCommand command) {
        MandateExecution execution = this.mandateExecutionRepository.findById(command.getExecutionId())
                .orElseThrow(() -> this.notFound("Mandate collection not found"));
        PaymentMandate mandate = this.paymentMandateRepository.findByIdForUpdate(execution.getMandateId())
                .orElseThrow(() -> this.notFound("Mandate not found"));

        LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
        RecallDecision decision = MandateRecallPolicy.evaluate(mandate.getKind(), execution.getStatus(), execution.getTransactionId(),
                execution.getCollectionDate(), businessDate, command.getReason(), this.bankHolidayCalendar);
        if (!decision.allowed()) {
            throw new ApiResourceException(decision.detail(), HttpStatus.CONFLICT, this.httpServletRequest);
        }

        String description = MandateNotice.reversalDescription(mandate, execution, command.getReason());
        TransactionView reversal = this.transactionService.reverse(this.reversal(execution.getTransactionId(),
                MandateIdempotency.reversalKey(execution.getId()), description));

        Long feeReversalId = null;
        if (execution.getFeeTransactionId() != null) {
            String feeDescription = MandateTexts.clip("Collection fee recall " + mandate.getReference(), 255);
            feeReversalId = this.transactionService.reverse(this.reversal(execution.getFeeTransactionId(),
                    MandateIdempotency.feeReversalKey(execution.getId()), feeDescription)).getId();
        }

        execution.setStatus(ExecutionStatus.REVERSED);
        execution.setRecallReason(command.getReason());
        execution.setReversalTransactionId(reversal.getId());
        execution.setFeeReversalTransactionId(feeReversalId);
        execution.setFailureDetail(MandateTexts.clip(command.getDetail(), 500));
        MandateExecution saved = this.mandateExecutionRepository.save(execution);

        this.applicationEventPublisher.publishEvent(new MandateExecutionRecalledEvent(mandate.getId(), saved.getId(), command.getReason().name()));
        log.info("Recalled mandate {} execution {} as {}", mandate.getReference(), saved.getId(), command.getReason());
        return this.mandateMapper.toExecutionView(saved);
    }

    private ReverseTransactionCommand reversal(Long transactionId, String idempotencyKey, String description) {
        ReverseTransactionCommand command = new ReverseTransactionCommand();
        command.setTransactionId(transactionId);
        command.setIdempotencyKey(idempotencyKey);
        command.setDescription(description);
        return command;
    }

    private ApiResourceException notFound(String message) {
        return new ApiResourceException(message, HttpStatus.NOT_FOUND, this.httpServletRequest);
    }
}
