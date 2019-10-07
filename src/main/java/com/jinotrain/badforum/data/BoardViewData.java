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

    // used when displaying boards
    public BoardViewData(long id, String name, long threadCount, long postCount)
    {
        this.id          = id;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
    }

    // used when displaying a thread, for a link back to its containing board
    public BoardViewData(long id, String name)
    {
        this(id, name, 0, 0);
    }
}
