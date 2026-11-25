package io.github.hossensyedriadh.cbs.mandate.internal.domain;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "mandate_executions", schema = "mandate", indexes = {
        @Index(name = "idx_mandate_executions_mandate", columnList = "mandate_id"),
        @Index(name = "idx_mandate_executions_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_mandate_exec_date", columnNames = {"mandate_id", "collection_date"})
})
public class MandateExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mandate_id", updatable = false, nullable = false)
    private Long mandateId;

    @Column(name = "collection_date", updatable = false, nullable = false)
    private LocalDate collectionDate;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "fee_amount", precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "skip_reason", length = 40)
    private ExecutionSkipReason skipReason;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "transaction_reference", length = 64)
    private String transactionReference;

    @Column(name = "fee_transaction_id")
    private Long feeTransactionId;

    @Column(name = "reversal_transaction_id")
    private Long reversalTransactionId;

    @Column(name = "fee_reversal_transaction_id")
    private Long feeReversalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recall_reason", length = 40)
    private MandateRecallReason recallReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "failure_detail", length = 500)
    private String failureDetail;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_attempt_on")
    private LocalDate lastAttemptOn;

    @Column(name = "attempted_at")
    private OffsetDateTime attemptedAt;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
