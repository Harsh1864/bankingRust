package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmendmentKind;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter
@Setter
public class MandateAmendmentView implements Serializable {
    @Serial
    private static final long serialVersionUID = 3901827465548392017L;

    private Long id;

    @JsonProperty("mandate_id")
    private Long mandateId;

    private AmendmentKind kind;

    @JsonProperty("previous_value")
    private String previousValue;

    @JsonProperty("new_value")
    private String newValue;

    private String reason;

    @JsonProperty("amended_at")
    private OffsetDateTime amendedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
