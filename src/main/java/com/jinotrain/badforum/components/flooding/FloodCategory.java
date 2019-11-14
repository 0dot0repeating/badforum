package com.jinotrain.badforum.components.flooding;

import java.time.Duration;

public enum FloodCategory
{
    ANY        ("View",         "view",      10,  Duration.ofSeconds(1)),  // basically exists to stop DoS attempts from hitting the database too hard
    LOGIN      ("Login",        "login",     100, Duration.ofMinutes(1)),
    REGISTER   ("Registration", "register",  100, Duration.ofHours(1)),
    POST_TOPIC ("New topic",    "posttopic", 2,   Duration.ofMinutes(1)),
    REPLY      ("Reply",        "reply",     4,   Duration.ofMinutes(1));

    public final String   niceName;
    public final String   propertyName;
    public final int      defaultLimit;
    public final Duration defaultWindow;

    FloodCategory(String name, String property, int limit, Duration window)
    {
        niceName      = name;
        propertyName  = property;
        defaultLimit  = limit;
        defaultWindow = window;
    }
}
