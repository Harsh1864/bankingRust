package io.github.hossensyedriadh.cbs.mandate.api.event;

public record MandateExecutionFailedEvent(Long mandateId, Long executionId, String reason) {
}
