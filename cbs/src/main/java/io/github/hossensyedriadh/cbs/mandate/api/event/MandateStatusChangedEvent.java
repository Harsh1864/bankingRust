package io.github.hossensyedriadh.cbs.mandate.api.event;

public record MandateStatusChangedEvent(Long mandateId, String fromStatus, String toStatus, String reason) {
}
