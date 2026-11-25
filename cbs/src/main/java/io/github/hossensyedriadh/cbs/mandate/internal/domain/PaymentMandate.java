package io.github.hossensyedriadh.cbs.mandate.internal.domain;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmountType;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CapBehaviour;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.CreditorScheme;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.HolidayAdjustment;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateKind;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.PaymentFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "payment_mandates", schema = "mandate", indexes = {
        @Index(name = "idx_payment_mandates_debtor", columnList = "debtor_account_id"),
        @Index(name = "idx_payment_mandates_creditor", columnList = "creditor_account_id"),
        @Index(name = "idx_payment_mandates_due", columnList = "status, next_collection_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_mandates_reference", columnNames = {"reference"})
})
public class PaymentMandate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "reference", updatable = false, nullable = false, length = 50)
    private String reference;

    @Column(name = "debtor_account_id", updatable = false, nullable = false)
    private Long debtorAccountId;

    @Column(name = "creditor_account_id")
    private Long creditorAccountId;

    @Column(name = "currency_id", nullable = false)
    private Long currencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", updatable = false, nullable = false, length = 30)
    private MandateKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MandateStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "creditor_scheme", nullable = false, length = 30)
    private CreditorScheme creditorScheme;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private PaymentFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_type", nullable = false, length = 30)
    private AmountType amountType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "start_date", updatable = false, nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "schedule_anchor")
    private LocalDate scheduleAnchor;

    @Column(name = "next_collection_date")
    private LocalDate nextCollectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_adjustment", nullable = false, length = 20)
    private HolidayAdjustment holidayAdjustment;

    @Enumerated(EnumType.STRING)
    @Column(name = "cap_behaviour", nullable = false, length = 20)
    private CapBehaviour capBehaviour;

    @Column(name = "min_amount", precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "monthly_cap", precision = 19, scale = 2)
    private BigDecimal monthlyCap;

    @Column(name = "collection_fee", precision = 19, scale = 2)
    private BigDecimal collectionFee;

    @Column(name = "failure_count", nullable = false)
    private Integer failureCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "external_creditor_name", length = 140)
    private String externalCreditorName;

    @Column(name = "external_creditor_iban", length = 34)
    private String externalCreditorIban;

    @Column(name = "narrative", length = 255)
    private String narrative;

    @Column(name = "opened_at", updatable = false, nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
