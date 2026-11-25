package io.github.hossensyedriadh.cbs.mandate.internal.impl;

import io.github.hossensyedriadh.cbs.eod.api.service.BusinessDateService;
import io.github.hossensyedriadh.cbs.exception.ApiResourceException;
import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateCollectionBatch;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateExecutionService;
import io.github.hossensyedriadh.cbs.mandate.internal.mapper.MandateMapper;
import io.github.hossensyedriadh.cbs.mandate.internal.outcome.CollectionTally;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.MandateExecutionRepository;
import io.github.hossensyedriadh.cbs.mandate.internal.repository.PaymentMandateRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Log4j2
@Service
@Transactional(readOnly = true)
public class MandateExecutionServiceImpl implements MandateExecutionService {
    private final PaymentMandateRepository paymentMandateRepository;
    private final MandateExecutionRepository mandateExecutionRepository;
    private final MandateCollectionWorker mandateCollectionWorker;
    private final BusinessDateService businessDateService;
    private final MandateMapper mandateMapper;
    private final HttpServletRequest httpServletRequest;

    @Autowired
    public MandateExecutionServiceImpl(PaymentMandateRepository paymentMandateRepository, MandateExecutionRepository mandateExecutionRepository,
                                       MandateCollectionWorker mandateCollectionWorker, BusinessDateService businessDateService,
                                       MandateMapper mandateMapper, HttpServletRequest httpServletRequest) {
        this.paymentMandateRepository = paymentMandateRepository;
        this.mandateExecutionRepository = mandateExecutionRepository;
        this.mandateCollectionWorker = mandateCollectionWorker;
        this.businessDateService = businessDateService;
        this.mandateMapper = mandateMapper;
        this.httpServletRequest = httpServletRequest;
    }

    @Override
    public List<MandateExecutionView> getExecutions(Long mandateId) {
        if (this.paymentMandateRepository.findById(mandateId).isEmpty()) {
            throw new ApiResourceException("Mandate not found", HttpStatus.NOT_FOUND, this.httpServletRequest);
        }
        return this.mandateExecutionRepository.findAllByMandateIdOrderByCollectionDateDesc(mandateId).stream()
                .map(this.mandateMapper::toExecutionView)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MandateCollectionBatch collectDue(@Nullable LocalDate businessDate) {
        LocalDate date = businessDate == null ? this.businessDateService.getCurrentBusinessDate() : businessDate;
        List<Long> mandateIds = this.paymentMandateRepository.findCollectibleIds(MandateStatus.ACTIVE, MandateStatus.PENDING, date);

        int posted = 0;
        int skipped = 0;
        int failed = 0;
        for (Long mandateId : mandateIds) {
            CollectionTally tally = this.mandateCollectionWorker.collect(mandateId, date);
            posted += tally.posted();
            skipped += tally.skipped();
            failed += tally.failed();
        }

        log.info("Mandate collection for {} examined {} posted {} skipped {} failed {}", date, mandateIds.size(), posted, skipped, failed);
        return new MandateCollectionBatch(date, mandateIds.size(), posted, skipped, failed);
    }
}
