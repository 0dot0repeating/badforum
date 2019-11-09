package com.jinotrain.badforum.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ThreadViewData
{
    public long   index         = -1;
    public String topic         = null;
    public UserViewData  author = null;
    public BoardViewData board  = null;
    public boolean canModerate  = false;
    public boolean wasMoved     = false;

    public int   postCount = 0;
    public int[] postRange = {0, 0};
    public List<PostViewData> posts = null;
    public Instant creationTime     = null;
    public Instant lastUpdate       = null;


    // used when displaying boards - individual posts aren't needed then
    public ThreadViewData(long index, String topic, UserViewData author, int postCount, Instant creationTime, Instant lastUpdate, boolean wasMoved)
    {
        this.index        = index;
        this.topic        = topic;
        this.author       = author;
        this.wasMoved     = wasMoved;

        this.postCount    = postCount;
        this.creationTime = creationTime;
        this.lastUpdate   = lastUpdate;
    }

    // used when displaying thread contents - the board's just needed for linking back
    public ThreadViewData(long index, String topic, UserViewData author, BoardViewData board, List<PostViewData> postData, int postCount, int[] postRange)
    {
        this.index        = index;
        this.topic        = topic;
        this.author       = author;
        this.board        = board;
        this.posts        = postData;
        this.postCount    = postCount;
        this.postRange    = postRange;
    }
}
