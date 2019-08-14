package com.jinotrain.badforum.components.flooding;

import java.time.Duration;

public enum FloodCategory
{
    LOGIN    (100, Duration.ofMinutes(1)),
    REGISTER (100, Duration.ofHours(1));

    public final int      defaultLimit;
    public final Duration defaultWindow;

    FloodCategory(int limit, Duration window)
    {
        defaultLimit  = limit;
        defaultWindow = window;
    }
}
