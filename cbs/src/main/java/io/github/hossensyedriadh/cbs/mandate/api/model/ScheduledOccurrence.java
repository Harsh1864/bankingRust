package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class ScheduledOccurrence implements Serializable {
    @Serial
    private static final long serialVersionUID = 7465192830475619283L;

    @JsonProperty("contractual_date")
    private LocalDate contractualDate;

    @JsonProperty("collection_date")
    private LocalDate collectionDate;

    private boolean skippedHoliday;
}
