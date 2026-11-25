package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.mandate.internal.outcome.CollectionOutcome;
import io.github.hossensyedriadh.cbs.mandate.internal.outcome.CollectionTally;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Log4j2
@Service
public class MandateCollectionWorker {
    static final int MAX_CATCH_UP = 6;

    private final MandatePoster mandatePoster;
    private final MandateFailureRecorder mandateFailureRecorder;

    @Autowired
    public MandateCollectionWorker(MandatePoster mandatePoster, MandateFailureRecorder mandateFailureRecorder) {
        this.mandatePoster = mandatePoster;
        this.mandateFailureRecorder = mandateFailureRecorder;
    }

    public CollectionTally collect(Long mandateId, LocalDate businessDate) {
        int posted = 0;
        int skipped = 0;
        int failed = 0;

        for (int attempt = 0; attempt < MAX_CATCH_UP; attempt++) {
            try {
                CollectionOutcome outcome = this.mandatePoster.post(mandateId, businessDate);
                switch (outcome) {
                    case POSTED -> posted++;
                    case SKIPPED -> skipped++;
                    case FAILED -> {
                        failed++;
                        return new CollectionTally(posted, skipped, failed);
                    }
                    case ADVANCED -> {
                    }
                    case IDLE -> {
                        return new CollectionTally(posted, skipped, failed);
                    }
                }
            } catch (RuntimeException exception) {
                log.warn("Mandate {} collection aborted: {}", mandateId, exception.getMessage());
                this.mandateFailureRecorder.recordUnexpected(mandateId, businessDate, exception);
                failed++;
                return new CollectionTally(posted, skipped, failed);
            }
        }

        return new CollectionTally(posted, skipped, failed);
    }
}
