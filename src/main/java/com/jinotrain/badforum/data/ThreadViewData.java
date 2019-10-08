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
    public BoardViewData board;

    private int    postCount;
    private List<PostViewData> posts = null;
    private Instant creationTime     = null;
    private Instant lastUpdate       = null;


    // used when displaying boards - individual posts aren't needed then
    public ThreadViewData(long id, String topic, UserViewData author, int postCount, Instant creationTime, Instant lastUpdate)
    {
        this.id           = id;
        this.topic        = topic;
        this.author       = author;
        this.board        = null;

        this.postCount    = postCount;
        this.creationTime = creationTime;
        this.lastUpdate   = lastUpdate;
    }

    // used when displaying thread contents - the board's just needed for linking back
    public ThreadViewData(long id, String topic, UserViewData author, BoardViewData board, List<PostViewData> posts)
    {
        this.id           = id;
        this.topic        = topic;
        this.author       = author;
        this.board        = board;

        setPosts(posts);
    }


    public void setPosts(List<PostViewData> posts)
    {
        this.posts = new ArrayList<>(posts);
        this.posts.sort(Comparator.comparing(p -> p.postTime));

        this.postCount    = this.posts.size();

        if (this.postCount == 0)
        {
            this.creationTime = null;
            this.lastUpdate   = null;
        }
        else
        {
            this.creationTime = this.posts.get(0).postTime;
            this.lastUpdate   = this.posts.get(postCount-1).postTime;
        }
    }

    public int getPostCount() { return postCount; }
    public List<PostViewData> getPosts() { return posts; }
    public Instant getCreationTime() { return creationTime; }
    public Instant getLastUpdate() { return lastUpdate; }
}
