package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class MandateStatusHistoryView implements Serializable {
    @Serial
    private static final long serialVersionUID = 1283746501928374650L;

    private Long id;

    @JsonProperty("mandate_id")
    private Long mandateId;

    @JsonProperty("from_status")
    private MandateStatus fromStatus;

    @JsonProperty("to_status")
    private MandateStatus toStatus;

    private String reason;

    @JsonProperty("changed_at")
    private OffsetDateTime changedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
