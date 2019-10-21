package com.jinotrain.badforum.data;

import java.time.Instant;

public class PostViewData
{
    public long index;
    public String text;
    public UserViewData author;
    public Instant postTime;
    public Instant lastEditTime;
    public boolean deleted;

    public PostViewData(long index, String text, UserViewData author, Instant postTime, Instant lastEditTime, boolean deleted)
    {
        this.index        = index;
        this.text         = text;
        this.author       = author;
        this.postTime     = postTime;
        this.lastEditTime = lastEditTime;
        this.deleted      = deleted;
    }
}
