package com.jinotrain.badforum.components.flooding;

import java.time.Duration;

public enum FloodCategory
{
    ANY        ("View",  10, Duration.ofSeconds(1)),  // basically exists to stop DoS attempts from hitting the database too hard
    LOGIN      ("Login", 100, Duration.ofMinutes(1)),
    REGISTER   ("Registration", 100, Duration.ofHours(1)),
    POST_TOPIC ("New topic", 2, Duration.ofMinutes(1)),
    REPLY      ("Reply", 4, Duration.ofMinutes(1));

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
