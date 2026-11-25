package io.github.hossensyedriadh.cbs.mandate.api.event;

public record MandateOpenedEvent(Long mandateId, String reference, String kind) {
}
