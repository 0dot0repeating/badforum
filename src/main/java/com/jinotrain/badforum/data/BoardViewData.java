package com.jinotrain.badforum.data;

import java.util.List;

public class BoardViewData
{
    public long index;
    public String name;
    public long threadCount;
    public long postCount;

    public List<BoardViewData>  childBoards = null;
    public List<ThreadViewData> threads     = null;

    // used when displaying boards
    public BoardViewData(long index, String name, long threadCount, long postCount)
    {
        this.index       = index;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
    }

    // used when displaying a thread, for a link back to its containing board
    public BoardViewData(long index, String name)
    {
        this(index, name, 0, 0);
    }
}
