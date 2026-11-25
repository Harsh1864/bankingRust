package io.github.hossensyedriadh.cbs.mandate.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateRecallReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class RecallMandateExecutionCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 8473625190473826150L;

    @NotNull
    @JsonProperty("execution_id")
    private Long executionId;

    @NotNull
    private MandateRecallReason reason;

    @NotBlank
    @Size(max = 500)
    private String detail;
}
