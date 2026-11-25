package io.github.hossensyedriadh.cbs.mandate.api.event;

public record MandateExecutionRecalledEvent(Long mandateId, Long executionId, String reason) {
}
