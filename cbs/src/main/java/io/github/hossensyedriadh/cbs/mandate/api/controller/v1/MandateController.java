package io.github.hossensyedriadh.cbs.mandate.api.controller.v1;

import io.github.hossensyedriadh.cbs.mandate.api.command.AmendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.CancelMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.OpenMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.RecallMandateExecutionCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.ResumeMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.RunMandateCollectionCommand;
import io.github.hossensyedriadh.cbs.mandate.api.command.SuspendMandateCommand;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateAmendmentView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateCollectionBatch;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateExecutionView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateSchedulePreview;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateStatusHistoryView;
import io.github.hossensyedriadh.cbs.mandate.api.model.MandateView;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateExecutionService;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateRecallService;
import io.github.hossensyedriadh.cbs.mandate.api.service.MandateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/mandates/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Mandate API", description = "Standing orders, direct debits, and account sweeps")
public class MandateController {
    private final MandateService mandateService;
    private final MandateExecutionService mandateExecutionService;
    private final MandateRecallService mandateRecallService;

    @Autowired
    public MandateController(MandateService mandateService, MandateExecutionService mandateExecutionService,
                             MandateRecallService mandateRecallService) {
        this.mandateService = mandateService;
        this.mandateExecutionService = mandateExecutionService;
        this.mandateRecallService = mandateRecallService;
    }

    @InitBinder
    private void bind(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @Operation(summary = "Get a mandate", parameters = @Parameter(name = "id", description = "Mandate id", required = true),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mandate retrieved", content = @Content(schema = @Schema(implementation = MandateView.class))),
                    @ApiResponse(responseCode = "404", description = "Mandate not found", content = @Content)
            })
    @GetMapping("/{id}")
    public ResponseEntity<MandateView> getMandate(@PathVariable("id") Long mandateId) {
        Optional<MandateView> mandate = this.mandateService.getMandate(mandateId);
        return mandate.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "List mandates that debit an account",
            responses = @ApiResponse(responseCode = "200", description = "Mandates retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MandateView.class)))))
    @GetMapping("/account/{id}/debtor")
    public ResponseEntity<List<MandateView>> getDebtorMandates(@PathVariable("id") Long accountId) {
        List<MandateView> mandates = this.mandateService.getMandatesForDebtor(accountId);
        if (mandates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(mandates);
    }

    @Operation(summary = "List mandates that credit an account",
            responses = @ApiResponse(responseCode = "200", description = "Mandates retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MandateView.class)))))
    @GetMapping("/account/{id}/creditor")
    public ResponseEntity<List<MandateView>> getCreditorMandates(@PathVariable("id") Long accountId) {
        List<MandateView> mandates = this.mandateService.getMandatesForCreditor(accountId);
        if (mandates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(mandates);
    }

    @Operation(summary = "Open a mandate",
            responses = @ApiResponse(responseCode = "201", description = "Mandate opened",
                    content = @Content(schema = @Schema(implementation = MandateView.class))))
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MandateView> open(@Valid @RequestBody OpenMandateCommand command) {
        MandateView mandate = this.mandateService.open(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(mandate.getId()).toUri();
        return ResponseEntity.created(location).body(mandate);
    }

    @Operation(summary = "Amend a mandate",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Mandate amended", content = @Content(schema = @Schema(implementation = MandateView.class))),
                    @ApiResponse(responseCode = "409", description = "Mandate cannot be amended", content = @Content)
            })
    @PostMapping(value = "/amend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MandateView> amend(@Valid @RequestBody AmendMandateCommand command) {
        return ResponseEntity.ok(this.mandateService.amend(command));
    }

    @Operation(summary = "Suspend a mandate",
            responses = @ApiResponse(responseCode = "204", description = "Mandate suspended"))
    @PostMapping(value = "/suspend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> suspend(@Valid @RequestBody SuspendMandateCommand command) {
        this.mandateService.suspend(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resume a suspended mandate",
            responses = @ApiResponse(responseCode = "204", description = "Mandate resumed"))
    @PostMapping(value = "/resume", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resume(@Valid @RequestBody ResumeMandateCommand command) {
        this.mandateService.resume(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel a mandate",
            responses = @ApiResponse(responseCode = "204", description = "Mandate cancelled"))
    @PostMapping(value = "/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cancel(@Valid @RequestBody CancelMandateCommand command) {
        this.mandateService.cancel(command);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Preview upcoming collection dates")
    @GetMapping("/{id}/schedule")
    public ResponseEntity<MandateSchedulePreview> preview(@PathVariable("id") Long mandateId,
                                                          @RequestParam(name = "occurrences", defaultValue = "12") int occurrences) {
        return ResponseEntity.ok(this.mandateService.preview(mandateId, occurrences));
    }

    @Operation(summary = "List amendments")
    @GetMapping("/{id}/amendments")
    public ResponseEntity<List<MandateAmendmentView>> amendments(@PathVariable("id") Long mandateId) {
        List<MandateAmendmentView> amendments = this.mandateService.getAmendments(mandateId);
        if (amendments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(amendments);
    }

    @Operation(summary = "List status history")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<MandateStatusHistoryView>> history(@PathVariable("id") Long mandateId) {
        List<MandateStatusHistoryView> history = this.mandateService.getStatusHistory(mandateId);
        if (history.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "List collection attempts")
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<MandateExecutionView>> executions(@PathVariable("id") Long mandateId) {
        List<MandateExecutionView> executions = this.mandateExecutionService.getExecutions(mandateId);
        if (executions.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(executions);
    }

    @Operation(summary = "Run due collections for a business date",
            responses = @ApiResponse(responseCode = "200", description = "Collection batch finished",
                    content = @Content(schema = @Schema(implementation = MandateCollectionBatch.class))))
    @PostMapping(value = "/collections/run", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MandateCollectionBatch> run(@RequestBody(required = false) RunMandateCollectionCommand command) {
        LocalDate businessDate = command == null ? null : command.getBusinessDate();
        return ResponseEntity.ok(this.mandateExecutionService.collectDue(businessDate));
    }

    @Operation(summary = "Recall a posted collection",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Collection recalled",
                            content = @Content(schema = @Schema(implementation = MandateExecutionView.class))),
                    @ApiResponse(responseCode = "409", description = "Collection cannot be recalled", content = @Content)
            })
    @PostMapping(value = "/executions/recall", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MandateExecutionView> recall(@Valid @RequestBody RecallMandateExecutionCommand command) {
        return ResponseEntity.ok(this.mandateRecallService.recall(command));
    }
}
