package io.github.hossensyedriadh.cbs.mandate.internal.domain;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Getter
@Setter
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "mandate_status_history", schema = "mandate", indexes = {
        @Index(name = "idx_mandate_status_history_mandate", columnList = "mandate_id")
})
public class MandateStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mandate_id", updatable = false, nullable = false)
    private Long mandateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", updatable = false, length = 20)
    private MandateStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", updatable = false, nullable = false, length = 20)
    private MandateStatus toStatus;

    @Column(name = "reason", updatable = false, length = 500)
    private String reason;

    @Column(name = "changed_at", updatable = false, nullable = false)
    private OffsetDateTime changedAt;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
