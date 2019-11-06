package com.jinotrain.badforum.db;

public enum BoardPermission
{
    VIEW("View posts"),
    POST("Create posts"),
    MODERATE("Moderate posts/threads"),
    ;

    public final String label;

    BoardPermission(String label)
    {
        this.label = label;
    }
}
