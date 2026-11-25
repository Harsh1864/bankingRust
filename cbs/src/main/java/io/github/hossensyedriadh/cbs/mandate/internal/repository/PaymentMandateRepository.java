package io.github.hossensyedriadh.cbs.mandate.internal.repository;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMandateRepository extends JpaRepository<PaymentMandate, Long> {
    List<PaymentMandate> findAllByDebtorAccountId(Long debtorAccountId);

    List<PaymentMandate> findAllByCreditorAccountId(Long creditorAccountId);

    Optional<PaymentMandate> findByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from PaymentMandate m where m.id = :id")
    Optional<PaymentMandate> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select m.id
            from PaymentMandate m
            where (m.status = :active
                   and m.nextCollectionDate is not null
                   and m.nextCollectionDate <= :businessDate)
               or (m.status = :pending
                   and m.startDate <= :businessDate)
            order by m.id
            """)
    List<Long> findCollectibleIds(@Param("active") MandateStatus active,
                                  @Param("pending") MandateStatus pending,
                                  @Param("businessDate") LocalDate businessDate);
}
