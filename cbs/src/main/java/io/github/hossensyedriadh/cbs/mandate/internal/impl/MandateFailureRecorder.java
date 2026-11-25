package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionFailedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateStatusChangedEvent;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateExecutionRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.PaymentMandateRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTexts;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTransitions;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Log4j2
@Service
public class MandateFailureRecorder {
    private final PaymentMandateRepository paymentMandateRepository;
    private final MandateExecutionRepository mandateExecutionRepository;
    private final MandateHistoryWriter mandateHistoryWriter;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public MandateFailureRecorder(PaymentMandateRepository paymentMandateRepository, MandateExecutionRepository mandateExecutionRepository,
                                  MandateHistoryWriter mandateHistoryWriter, ApplicationEventPublisher applicationEventPublisher) {
        this.paymentMandateRepository = paymentMandateRepository;
        this.mandateExecutionRepository = mandateExecutionRepository;
        this.mandateHistoryWriter = mandateHistoryWriter;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUnexpected(Long mandateId, LocalDate businessDate, RuntimeException exception) {
        try {
            PaymentMandate mandate = this.paymentMandateRepository.findByIdForUpdate(mandateId).orElse(null);
            if (mandate == null || mandate.getScheduleAnchor() == null) {
                return;
            }

            MandateExecution execution = this.mandateExecutionRepository
                    .findByMandateIdAndCollectionDate(mandateId, mandate.getScheduleAnchor())
                    .orElseGet(() -> {
                        MandateExecution created = new MandateExecution();
                        created.setMandateId(mandateId);
                        created.setCollectionDate(mandate.getScheduleAnchor());
                        created.setAttemptCount(0);
                        return created;
                    });

            if (execution.getStatus() == ExecutionStatus.POSTED) {
                return;
            }

            String detail = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setSkipReason(ExecutionSkipReason.UNEXPECTED);
            execution.setFailureDetail(MandateTexts.clip(detail, 500));
            execution.setScheduledDate(mandate.getNextCollectionDate());
            execution.setLastAttemptOn(businessDate);
            execution.setAttemptedAt(OffsetDateTime.now());
            execution.setAttemptCount((execution.getAttemptCount() == null ? 0 : execution.getAttemptCount()) + 1);
            MandateExecution saved = this.mandateExecutionRepository.save(execution);

            int failures = (mandate.getFailureCount() == null ? 0 : mandate.getFailureCount()) + 1;
            mandate.setFailureCount(failures);
            int maxRetries = mandate.getMaxRetries() == null ? 3 : mandate.getMaxRetries();
            if (failures >= maxRetries && MandateTransitions.canSuspend(mandate.getStatus())) {
                MandateStatus from = mandate.getStatus();
                mandate.setStatus(MandateStatus.SUSPENDED);
                mandate.setSuspendedAt(OffsetDateTime.now());
                this.mandateHistoryWriter.status(mandate.getId(), from, mandate.getStatus(), "Retry limit reached after an unexpected collection failure");
                this.applicationEventPublisher.publishEvent(new MandateStatusChangedEvent(mandate.getId(), from.name(),
                        mandate.getStatus().name(), "Retry limit reached after an unexpected collection failure"));
            }

            this.paymentMandateRepository.save(mandate);
            this.applicationEventPublisher.publishEvent(new MandateExecutionFailedEvent(mandate.getId(), saved.getId(), ExecutionSkipReason.UNEXPECTED.name()));
            log.warn("Recorded unexpected collection failure for mandate {}: {}", mandate.getReference(), detail);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Unexpected failure for mandate {} was already recorded", mandateId);
        }
    }
}
