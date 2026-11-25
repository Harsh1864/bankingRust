package io.github.hossensyedriadh.cbs.mandate.internal.domain;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.AmendmentKind;
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
@Table(name = "mandate_amendments", schema = "mandate", indexes = {
        @Index(name = "idx_mandate_amendments_mandate", columnList = "mandate_id")
})
public class MandateAmendment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mandate_id", updatable = false, nullable = false)
    private Long mandateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", updatable = false, nullable = false, length = 30)
    private AmendmentKind kind;

    @Column(name = "previous_value", updatable = false, length = 255)
    private String previousValue;

    @Column(name = "new_value", updatable = false, length = 255)
    private String newValue;

    @Column(name = "reason", updatable = false, nullable = false, length = 500)
    private String reason;

    @Column(name = "amended_at", updatable = false, nullable = false)
    private OffsetDateTime amendedAt;

    @Setter(AccessLevel.NONE)
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}
