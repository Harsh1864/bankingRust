package io.github.hossensyedriadh.cbs.mandate.internal.repository;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.ExecutionStatus;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MandateExecutionRepository extends JpaRepository<MandateExecution, Long> {
    List<MandateExecution> findAllByMandateIdOrderByCollectionDateDesc(Long mandateId);

    Optional<MandateExecution> findByMandateIdAndCollectionDate(Long mandateId, LocalDate collectionDate);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from MandateExecution e
            where e.mandateId = :mandateId
              and e.status = :status
              and e.collectionDate >= :from
              and e.collectionDate < :to
            """)
    BigDecimal sumAmount(@Param("mandateId") Long mandateId,
                         @Param("status") ExecutionStatus status,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}
