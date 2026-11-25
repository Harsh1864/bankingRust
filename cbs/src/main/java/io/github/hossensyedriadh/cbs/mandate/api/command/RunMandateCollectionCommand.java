package io.github.hossensyedriadh.cbs.mandate.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
public class RunMandateCollectionCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 5582019384756102938L;

    @JsonProperty("business_date")
    private LocalDate businessDate;
}
