package io.github.hossensyedriadh.cbs.mandate.internal.mapper;

import io.github.hossensyedriadh.cbs.configuration.mapper.MapperConfiguration;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateAmendmentView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateStatusHistoryView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateView;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateAmendment;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateExecution;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.MandateStatusHistory;
import io.github.hossensyedriadh.cbs.mandate.internal.domain.PaymentMandate;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfiguration.class)
public interface MandateMapper {
    MandateView toView(PaymentMandate entity);

    MandateExecutionView toExecutionView(MandateExecution entity);

    MandateAmendmentView toAmendmentView(MandateAmendment entity);

    MandateStatusHistoryView toHistoryView(MandateStatusHistory entity);
}
