package org.schabi.newpipe.extractor.derbis;

import javax.annotation.Nonnull;

public final class DerbisSponsorBlockSegment {
    private final String category;
    private final double startTime;
    private final double endTime;

    public DerbisSponsorBlockSegment(@Nonnull final String category, final double startTime, final double endTime) {
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Nonnull
    public String getCategory() {
        return category;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }
}
