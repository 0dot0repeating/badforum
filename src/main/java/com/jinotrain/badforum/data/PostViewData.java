package com.jinotrain.badforum.data;

import java.time.Instant;

public class PostViewData
{
    public long id;
    public String text;
    public UserViewData author;
    public Instant postTime;
    public Instant lastEditTime;
}
