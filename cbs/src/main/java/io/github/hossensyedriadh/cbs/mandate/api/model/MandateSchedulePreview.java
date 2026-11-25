package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class MandateSchedulePreview implements Serializable {
    @Serial
    private static final long serialVersionUID = 6574839201746503921L;

    @JsonProperty("mandate_id")
    private Long mandateId;

    private List<ScheduledOccurrence> occurrences;
}
