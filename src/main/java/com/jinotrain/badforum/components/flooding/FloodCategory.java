package com.jinotrain.badforum.components.flooding;

import java.time.Duration;

public enum FloodCategory
{
    LOGIN    ("Login", 100, Duration.ofMinutes(1)),
    REGISTER ("Registration", 100, Duration.ofHours(1));

    public final String   niceName;
    public final int      defaultLimit;
    public final Duration defaultWindow;

    FloodCategory(String name, int limit, Duration window)
    {
        niceName      = name;
        defaultLimit  = limit;
        defaultWindow = window;
    }
}
