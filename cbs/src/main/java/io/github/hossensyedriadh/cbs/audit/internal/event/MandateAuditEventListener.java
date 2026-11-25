package io.github.hossensyedriadh.cbs.audit.internal.event;

import io.github.hossensyedriadh.cbs.audit.api.AuditService;
import io.github.hossensyedriadh.cbs.audit.api.enumerator.AuditAction;
import io.github.hossensyedriadh.cbs.audit.api.model.CreateAuditLogCommand;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateAmendedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionFailedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionPostedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateExecutionRecalledEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateOpenedEvent;
import io.github.hossensyedriadh.cbs.mandate.api.event.MandateStatusChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class MandateAuditEventListener {
    private static final String MODULE_NAME = "MANDATE";
    private static final String MANDATE_ENTITY = "PAYMENT_MANDATE";
    private static final String EXECUTION_ENTITY = "MANDATE_EXECUTION";

    private final AuditService auditService;

    @Autowired
    public MandateAuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @ApplicationModuleListener
    public void on(MandateOpenedEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, MANDATE_ENTITY, event.mandateId(),
                AuditAction.CREATED, "Mandate " + event.reference() + " opened as " + event.kind()));
    }

    @ApplicationModuleListener
    public void on(MandateAmendedEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, MANDATE_ENTITY, event.mandateId(),
                AuditAction.UPDATED, "Mandate amended: " + event.amendmentKind()));
    }

    @ApplicationModuleListener
    public void on(MandateStatusChangedEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, MANDATE_ENTITY, event.mandateId(),
                this.actionFor(event.toStatus()), event.fromStatus() + " -> " + event.toStatus() + ": " + event.reason()));
    }

    @ApplicationModuleListener
    public void on(MandateExecutionPostedEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, EXECUTION_ENTITY, event.executionId(),
                AuditAction.POSTED, "Collection posted for mandate " + event.mandateId() + " amount " + event.amount()));
    }

    @ApplicationModuleListener
    public void on(MandateExecutionFailedEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, EXECUTION_ENTITY, event.executionId(),
                AuditAction.FAILED, "Collection failed for mandate " + event.mandateId() + ": " + event.reason()));
    }

    @ApplicationModuleListener
    public void on(MandateExecutionRecalledEvent event) {
        this.auditService.record(new CreateAuditLogCommand(MODULE_NAME, EXECUTION_ENTITY, event.executionId(),
                AuditAction.REVERSED, "Collection recalled for mandate " + event.mandateId() + ": " + event.reason()));
    }

    private AuditAction actionFor(String status) {
        return switch (status) {
            case "CANCELLED" -> AuditAction.CANCELLED;
            case "SUSPENDED" -> AuditAction.BLOCKED;
            case "ACTIVE" -> AuditAction.ACTIVATED;
            case "COMPLETED" -> AuditAction.COMPLETED;
            case "EXPIRED" -> AuditAction.CLOSED;
            default -> AuditAction.UPDATED;
        };
    }
}
