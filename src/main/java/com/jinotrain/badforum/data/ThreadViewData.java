package com.jinotrain.badforum.data;

import java.time.Instant;
import java.util.List;

public class ThreadViewData
{
    public long   id;
    public String topic;
    public UserViewData author;
    public long postCount;

    public List<PostViewData> posts = null;
    public Instant creationTime     = null;
    public Instant lastUpdate       = null;


    public ThreadViewData(long id, String topic, UserViewData author, long postCount)
    {
        this.id           = id;
        this.topic        = topic;
        this.author       = author;
        this.postCount    = postCount;
    }
}
