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
    public boolean canModerate;

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

    // used when displaying a thread (for the backlink), and when moving/splitting a thread (for displaying the board tree)
    public BoardViewData(long index, String name)
    {
        this(index, name, 0, 0, false);
    }
}
