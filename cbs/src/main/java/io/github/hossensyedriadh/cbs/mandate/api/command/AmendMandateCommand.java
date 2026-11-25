package io.github.hossensyedriadh.cbs.mandate.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AmendMandateCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 7731902846519028374L;

    @NotNull
    @JsonProperty("mandate_id")
    private Long mandateId;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @Positive
    private BigDecimal amount;

    private PaymentFrequency frequency;

    @Min(1)
    @Max(31)
    @JsonProperty("day_of_month")
    private Integer dayOfMonth;

    @Min(1)
    @Max(7)
    @JsonProperty("day_of_week")
    private Integer dayOfWeek;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("clear_end_date")
    private Boolean clearEndDate;

    @JsonProperty("holiday_adjustment")
    private HolidayAdjustment holidayAdjustment;

    @JsonProperty("cap_behaviour")
    private CapBehaviour capBehaviour;

    @Positive
    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("clear_min_amount")
    private Boolean clearMinAmount;

    @Positive
    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @JsonProperty("clear_max_amount")
    private Boolean clearMaxAmount;

    @Positive
    @JsonProperty("monthly_cap")
    private BigDecimal monthlyCap;

    @JsonProperty("clear_monthly_cap")
    private Boolean clearMonthlyCap;

    @Positive
    @JsonProperty("collection_fee")
    private BigDecimal collectionFee;

    @JsonProperty("clear_collection_fee")
    private Boolean clearCollectionFee;

    @Min(0)
    @Max(10)
    @JsonProperty("max_retries")
    private Integer maxRetries;

    @JsonProperty("creditor_account_id")
    private Long creditorAccountId;

    @Size(max = 140)
    @JsonProperty("external_creditor_name")
    private String externalCreditorName;

    @Size(max = 34)
    @JsonProperty("external_creditor_iban")
    private String externalCreditorIban;

    @Size(max = 180)
    private String narrative;
}
