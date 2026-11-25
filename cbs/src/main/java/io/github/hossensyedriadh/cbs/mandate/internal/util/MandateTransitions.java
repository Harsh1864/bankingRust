package io.github.hossensyedriadh.cbs.mandate.internal.util;

import io.github.hossensyedriadh.cbs.mandate.api.enumerator.MandateStatus;

public final class MandateTransitions {
    private MandateTransitions() {
    }

    public static boolean canAmend(MandateStatus status) {
        return status == MandateStatus.PENDING || status == MandateStatus.ACTIVE || status == MandateStatus.SUSPENDED;
    }

    public static boolean canSuspend(MandateStatus status) {
        return status == MandateStatus.PENDING || status == MandateStatus.ACTIVE;
    }

    public static boolean canResume(MandateStatus status) {
        return status == MandateStatus.SUSPENDED;
    }

    public static boolean canCancel(MandateStatus status) {
        return status == MandateStatus.PENDING || status == MandateStatus.ACTIVE || status == MandateStatus.SUSPENDED;
    }

    public static boolean canCollect(MandateStatus status) {
        return status == MandateStatus.ACTIVE;
    }

    public static boolean isTerminal(MandateStatus status) {
        return status == MandateStatus.CANCELLED || status == MandateStatus.COMPLETED || status == MandateStatus.EXPIRED;
    }
}
