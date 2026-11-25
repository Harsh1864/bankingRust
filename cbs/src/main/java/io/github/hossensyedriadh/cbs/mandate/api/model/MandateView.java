package io.github.hossensyedriadh.cbs.mandate.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
public class MandateView implements Serializable {
    @Serial
    private static final long serialVersionUID = 2847591036485720193L;

    private Long id;

    private String reference;

    @JsonProperty("debtor_account_id")
    private Long debtorAccountId;

    @JsonProperty("creditor_account_id")
    private Long creditorAccountId;

    @JsonProperty("currency_id")
    private Long currencyId;

    private MandateKind kind;

    private MandateStatus status;

    @JsonProperty("creditor_scheme")
    private CreditorScheme creditorScheme;

    private PaymentFrequency frequency;

    @JsonProperty("amount_type")
    private AmountType amountType;

    private BigDecimal amount;

    @JsonProperty("day_of_month")
    private Integer dayOfMonth;

    @JsonProperty("day_of_week")
    private Integer dayOfWeek;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("schedule_anchor")
    private LocalDate scheduleAnchor;

    @JsonProperty("next_collection_date")
    private LocalDate nextCollectionDate;

    @JsonProperty("holiday_adjustment")
    private HolidayAdjustment holidayAdjustment;

    @JsonProperty("cap_behaviour")
    private CapBehaviour capBehaviour;

    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @JsonProperty("monthly_cap")
    private BigDecimal monthlyCap;

    @JsonProperty("collection_fee")
    private BigDecimal collectionFee;

    @JsonProperty("failure_count")
    private Integer failureCount;

    @JsonProperty("max_retries")
    private Integer maxRetries;

    @JsonProperty("external_creditor_name")
    private String externalCreditorName;

    @JsonProperty("external_creditor_iban")
    private String externalCreditorIban;

    private String narrative;

    @JsonProperty("opened_at")
    private OffsetDateTime openedAt;

    @JsonProperty("suspended_at")
    private OffsetDateTime suspendedAt;

    @JsonProperty("cancelled_at")
    private OffsetDateTime cancelledAt;

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
}
