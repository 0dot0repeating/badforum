package com.jinotrain.badforum.db;

public enum BoardPermission
{
    VIEW("View posts"),
    POST("Create posts"),
    MODERATE("Delete posts"),
    ;

    public final String label;

    BoardPermission(String label)
    {
        this.label = label;
    }
}
