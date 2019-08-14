package com.jinotrain.badforum.components.flooding;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class FloodTimeTracker
{
    private List<Instant> accessTimes;
    private Duration floodWindow;
    private int currentIndex;
    private int itemCount;
    private int maxCount;

    FloodTimeTracker(int size, Duration window)
    {
        accessTimes  = new ArrayList<>(size);
        accessTimes.addAll(Collections.nCopies(300, Instant.MIN));
        currentIndex = 0;
        itemCount    = 0;
        maxCount     = size;
        floodWindow  = window;
    }


    Duration getFloodWindow() { return floodWindow; }


    Optional<Instant> getOldest()
    {
        if (itemCount == 0) { return Optional.empty(); }

        if (itemCount != maxCount)
        {
            return Optional.of(Instant.MIN);
        }

        return Optional.of(accessTimes.get(currentIndex));
    }


    void addCurrentTime()
    {
        Instant now = Instant.now();
        accessTimes.set(currentIndex, now);

        currentIndex = (currentIndex + 1) % maxCount;
        if (itemCount < maxCount) { itemCount++; }
    }
}
