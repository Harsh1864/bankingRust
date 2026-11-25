package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionSkipReason;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class MandateExecutionView implements Serializable {
    @Serial
    private static final long serialVersionUID = 6172839405162738490L;

    private Long id;

    @JsonProperty("mandate_id")
    private Long mandateId;

    @JsonProperty("collection_date")
    private LocalDate collectionDate;

    @JsonProperty("scheduled_date")
    private LocalDate scheduledDate;

    private BigDecimal amount;

    @JsonProperty("fee_amount")
    private BigDecimal feeAmount;

    private ExecutionStatus status;

    @JsonProperty("skip_reason")
    private ExecutionSkipReason skipReason;

    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("transaction_reference")
    private String transactionReference;

    @JsonProperty("fee_transaction_id")
    private Long feeTransactionId;

    @JsonProperty("reversal_transaction_id")
    private Long reversalTransactionId;

    @JsonProperty("fee_reversal_transaction_id")
    private Long feeReversalTransactionId;

    @JsonProperty("recall_reason")
    private MandateRecallReason recallReason;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("failure_detail")
    private String failureDetail;

    @JsonProperty("attempt_count")
    private Integer attemptCount;

    @JsonProperty("last_attempt_on")
    private LocalDate lastAttemptOn;

    @JsonProperty("attempted_at")
    private OffsetDateTime attemptedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
