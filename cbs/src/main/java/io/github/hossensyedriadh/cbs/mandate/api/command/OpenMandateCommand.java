package io.github.hossensyedriadh.cbs.mandate.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import io.github.hossensyedriadh.cbs.mandate.api.validation.ValidOpenMandate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@ValidOpenMandate
public class OpenMandateCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 4822176501938472215L;

    @NotNull
    @JsonProperty("debtor_account_id")
    private Long debtorAccountId;

    @JsonProperty("creditor_account_id")
    private Long creditorAccountId;

    @NotNull
    @JsonProperty("currency_id")
    private Long currencyId;

    @NotNull
    private MandateKind kind;

    @NotNull
    @JsonProperty("creditor_scheme")
    private CreditorScheme creditorScheme;

    @NotNull
    private PaymentFrequency frequency;

    @NotNull
    @JsonProperty("amount_type")
    private AmountType amountType;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Min(1)
    @Max(31)
    @JsonProperty("day_of_month")
    private Integer dayOfMonth;

    @Min(1)
    @Max(7)
    @JsonProperty("day_of_week")
    private Integer dayOfWeek;

    @NotNull
    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @NotNull
    @JsonProperty("holiday_adjustment")
    private HolidayAdjustment holidayAdjustment;

    @NotNull
    @JsonProperty("cap_behaviour")
    private CapBehaviour capBehaviour;

    @Positive
    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @Positive
    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @Positive
    @JsonProperty("monthly_cap")
    private BigDecimal monthlyCap;

    @Positive
    @JsonProperty("collection_fee")
    private BigDecimal collectionFee;

    @Min(0)
    @Max(10)
    @JsonProperty("max_retries")
    private Integer maxRetries;

    @Size(max = 140)
    @JsonProperty("external_creditor_name")
    private String externalCreditorName;

    @Size(max = 34)
    @JsonProperty("external_creditor_iban")
    private String externalCreditorIban;

    @Size(max = 180)
    private String narrative;
}
