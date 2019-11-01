package com.jinotrain.badforum.data;

import java.util.List;

public class BoardViewData
{
    public long index;
    public String name;
    public long threadCount;
    public long postCount;
    public boolean isRootBoard;
    public boolean canManage;

    public List<BoardViewData>  childBoards = null;
    public List<ThreadViewData> threads     = null;

    // used when displaying boards
    public BoardViewData(long index, String name, long threadCount, long postCount, boolean isRootBoard)
    {
        this.index       = index;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
        this.isRootBoard = isRootBoard;
    }

    // used when displaying a thread, for a link back to its containing board
    public BoardViewData(long index, String name)
    {
        this(index, name, 0, 0, false);
    }
}
