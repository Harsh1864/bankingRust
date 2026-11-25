package io.github.hossensyedriadh.cbs.mandate.api.service;

import io.github.hossensyedriadh.cbs.mandate.api.command.RecallMandateExecutionCommand;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import jakarta.validation.Valid;

public interface MandateRecallService {
    MandateExecutionView recall(@Valid RecallMandateExecutionCommand command);
}
