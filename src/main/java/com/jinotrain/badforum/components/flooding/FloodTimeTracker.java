package com.jinotrain.badforum.components.flooding;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FloodTimeTracker
{
    private List<Instant> accessTimes;
    private Duration floodWindow;
    private int maxCount;

    private int itemCount    =  0;
    private int lastIndex    = -1;
    private int currentIndex =  0;

    FloodTimeTracker(int size, Duration window)
    {
        accessTimes  = new ArrayList<>(size);
        accessTimes.addAll(Collections.nCopies(size, Instant.MIN));
        floodWindow  = window;
        maxCount     = size;
    }


    Duration getFloodWindow() { return floodWindow; }


    Instant getOldest()
    {
        if (itemCount == 0) { return null; }

        if (itemCount != maxCount)
        {
            return Instant.MIN;
        }

        return accessTimes.get(currentIndex);
    }


    Instant getNewest()
    {
        if (itemCount == 0) { return null; }
        return accessTimes.get(lastIndex);
    }


    void addCurrentTime()
    {
        Instant now = Instant.now();
        accessTimes.set(currentIndex, now);

        lastIndex    = currentIndex;
        currentIndex = (currentIndex + 1) % maxCount;
        if (itemCount < maxCount) { itemCount++; }
    }
}
