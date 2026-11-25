package io.github.hossensyedriadh.cbs.mandate.api.service;

import io.github.hossensyedriadh.cbs.mandate.api.model.MandateCollectionBatch;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

public interface MandateExecutionService {
    List<MandateExecutionView> getExecutions(Long mandateId);

    MandateCollectionBatch collectDue(@Nullable LocalDate businessDate);
}
