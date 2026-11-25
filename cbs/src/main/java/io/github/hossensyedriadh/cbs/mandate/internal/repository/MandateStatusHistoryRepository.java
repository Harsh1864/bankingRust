package io.github.hossensyedriadh.cbs.mandate.internal.repository;

import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MandateStatusHistoryRepository extends JpaRepository<MandateStatusHistory, Long> {
    List<MandateStatusHistory> findAllByMandateIdOrderByChangedAtDesc(Long mandateId);
}
