package com.jinotrain.badforum.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ThreadViewData
{
    public long   id;
    public String topic;
    public UserViewData author;

    private int    postCount;
    private List<PostViewData> posts = null;
    private Instant creationTime     = null;
    private Instant lastUpdate       = null;


    public ThreadViewData(long id, String topic, UserViewData author, int postCount, Instant creationTime, Instant lastUpdate)
    {
        this.id           = id;
        this.topic        = topic;
        this.author       = author;
        this.postCount    = postCount;
        this.creationTime = creationTime;
        this.lastUpdate   = lastUpdate;
    }

    public ThreadViewData(long id, String topic, UserViewData author, List<PostViewData> posts)
    {
        this.id           = id;
        this.topic        = topic;
        this.author       = author;
        setPosts(posts);
    }


    public void setPosts(List<PostViewData> posts)
    {
        this.posts = new ArrayList<>(posts);
        this.posts.sort(Comparator.comparing(p -> p.postTime));

        this.postCount    = this.posts.size();
        this.creationTime = this.posts.get(0).postTime;
        this.lastUpdate   = this.posts.get(postCount-1).postTime;
    }

    public int getPostCount() { return postCount; }
    public List<PostViewData> getPosts() { return posts; }
    public Instant getCreationTime() { return creationTime; }
    public Instant getLastUpdate() { return lastUpdate; }
}
