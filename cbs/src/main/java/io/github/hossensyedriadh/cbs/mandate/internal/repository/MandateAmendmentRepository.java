package io.github.hossensyedriadh.cbs.mandate.internal.repository;

import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateAmendment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MandateAmendmentRepository extends JpaRepository<MandateAmendment, Long> {
    List<MandateAmendment> findAllByMandateIdOrderByAmendedAtDesc(Long mandateId);
}
