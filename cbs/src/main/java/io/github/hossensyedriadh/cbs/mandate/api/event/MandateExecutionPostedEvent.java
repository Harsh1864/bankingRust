package io.github.hossensyedriadh.cbs.mandate.api.event;

public record MandateExecutionPostedEvent(Long mandateId, Long executionId, String amount) {
}
