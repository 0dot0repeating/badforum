package com.jinotrain.badforum.data;

import java.time.Instant;

public class PostViewData
{
    public long id;
    public String text;
    public UserViewData author;
    public Instant postTime;
    public Instant lastEditTime;

    public PostViewData(long id, String text, UserViewData author, Instant postTime, Instant lastEditTime)
    {
        this.id           = id;
        this.text         = text;
        this.author       = author;
        this.postTime     = postTime;
        this.lastEditTime = lastEditTime;
    }
}
