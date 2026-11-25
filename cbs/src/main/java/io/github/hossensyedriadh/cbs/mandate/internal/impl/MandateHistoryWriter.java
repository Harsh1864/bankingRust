package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateAmendment;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateStatusHistory;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateAmendmentRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateStatusHistoryRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateTexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class MandateHistoryWriter {
    private final MandateStatusHistoryRepository statusHistoryRepository;
    private final MandateAmendmentRepository amendmentRepository;

    @Autowired
    public MandateHistoryWriter(MandateStatusHistoryRepository statusHistoryRepository, MandateAmendmentRepository amendmentRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
        this.amendmentRepository = amendmentRepository;
    }

    public void status(Long mandateId, MandateStatus from, MandateStatus to, String reason) {
        if (from == to) {
            return;
        }

        MandateStatusHistory row = new MandateStatusHistory();
        row.setMandateId(mandateId);
        row.setFromStatus(from);
        row.setToStatus(to);
        row.setReason(MandateTexts.clip(reason, 500));
        row.setChangedAt(OffsetDateTime.now());
        this.statusHistoryRepository.save(row);
    }

    public void amendments(List<MandateAmendment> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        this.amendmentRepository.saveAll(rows);
    }
}
