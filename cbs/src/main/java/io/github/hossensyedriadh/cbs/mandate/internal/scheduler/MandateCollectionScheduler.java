package io.github.hossensyedriadh.cbs.mandate.internal.scheduler;

import io.github.hossensyedriadh.cbs.eod.api.service.BusinessDateService;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateCollectionBatch;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateExecutionService;
import io.github.hossensyedriadh.cbs.mandate.internal.util.MandateCollectionWindow;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Log4j2
@Service
public class MandateCollectionScheduler {
    private final MandateExecutionService mandateExecutionService;
    private final BusinessDateService businessDateService;

    @Autowired
    public MandateCollectionScheduler(MandateExecutionService mandateExecutionService, BusinessDateService businessDateService) {
        this.mandateExecutionService = mandateExecutionService;
        this.businessDateService = businessDateService;
    }

    @Scheduled(cron = "0 15 1 * * *")
    public void collectDueMandates() {
        LocalDate businessDate = this.businessDateService.getCurrentBusinessDate();
        LocalDate systemDate = LocalDate.now();
        String holdReason = MandateCollectionWindow.holdReason(businessDate, systemDate);
        if (holdReason != null) {
            log.warn("Skipping mandate collection for business date {}: {}", businessDate, holdReason);
            return;
        }
        log.info("Starting mandate collection for business date {}", businessDate.format(DateTimeFormatter.ISO_DATE));
        MandateCollectionBatch batch = this.mandateExecutionService.collectDue(businessDate);
        log.info("Mandate collection finished for {} examined {} posted {} skipped {} failed {}",
                batch.getBusinessDate(), batch.getExamined(), batch.getPosted(), batch.getSkipped(), batch.getFailed());
    }
}
