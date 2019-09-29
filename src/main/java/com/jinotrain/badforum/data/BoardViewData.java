package com.jinotrain.badforum.data;

import java.util.List;

public class BoardViewData
{
    public long   id;
    public String name;
    public long threadCount;
    public long postCount;

    public List<BoardViewData>  childBoards = null;
    public List<ThreadViewData> threads     = null;

    public BoardViewData(long id, String name, long threadCount, long postCount)
    {
        this.id          = id;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
    }
}
