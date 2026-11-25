package io.github.hossensyedriadh.cbs.mandate.api.service;

import io.github.hossensyedriadh.cbs.mandate.api.command.AmendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.CancelMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.OpenMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.ResumeMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.SuspendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateAmendmentView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateSchedulePreview;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateStatusHistoryView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateView;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface MandateService {
    Optional<MandateView> getMandate(Long id);

    List<MandateView> getMandatesForDebtor(Long accountId);

    List<MandateView> getMandatesForCreditor(Long accountId);

    List<MandateAmendmentView> getAmendments(Long mandateId);

    List<MandateStatusHistoryView> getStatusHistory(Long mandateId);

    MandateSchedulePreview preview(Long mandateId, int occurrences);

    MandateView open(@Valid OpenMandateCommand command);

    MandateView amend(@Valid AmendMandateCommand command);

    void suspend(@Valid SuspendMandateCommand command);

    void resume(@Valid ResumeMandateCommand command);

    void cancel(@Valid CancelMandateCommand command);
}
