package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MandateCollectionBatch implements Serializable {
    @Serial
    private static final long serialVersionUID = 8192736450192837465L;

    @JsonProperty("business_date")
    private LocalDate businessDate;

    private int examined;

    private int posted;

    private int skipped;

    private int failed;
}
