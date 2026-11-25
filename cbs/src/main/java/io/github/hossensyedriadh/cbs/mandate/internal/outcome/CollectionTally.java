package io.github.hossensyedriadh.cbs.mandate.internal.outcome;

public record CollectionTally(int posted, int skipped, int failed) {
    public static CollectionTally empty() {
        return new CollectionTally(0, 0, 0);
    }

    public CollectionTally plusPosted() {
        return new CollectionTally(this.posted + 1, this.skipped, this.failed);
    }

    public CollectionTally plusSkipped() {
        return new CollectionTally(this.posted, this.skipped + 1, this.failed);
    }

    public CollectionTally plusFailed() {
        return new CollectionTally(this.posted, this.skipped, this.failed + 1);
    }
}
