package io.github.hossensyedriadh.cbs.mandate.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class SuspendMandateCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 1193847562039485761L;

    @NotNull
    @JsonProperty("mandate_id")
    private Long mandateId;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
