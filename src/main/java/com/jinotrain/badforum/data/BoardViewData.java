package com.jinotrain.badforum.data;

import java.util.List;

public class BoardViewData
{
    public long index;
    public String name;
    public long threadCount;
    public long postCount;
    public long totalThreadCount;
    public long totalPostCount;
    public boolean isRootBoard;
    public boolean canManage;
    public boolean canModerate;

    public List<BoardViewData>  childBoards = null;
    public List<ThreadViewData> threads     = null;

    // used when displaying a given board
    public BoardViewData(long index, String name, long threadCount, long postCount, boolean isRootBoard)
    {
        this.index       = index;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
        this.isRootBoard = isRootBoard;
    }

    // used for storing sub-board data when displaying a given board
    public BoardViewData(long index, String name, long totalThreadCount, long totalPostCount)
    {
        this.index            = index;
        this.name             = name;
        this.totalThreadCount = totalThreadCount;
        this.totalPostCount   = totalPostCount;
    }

    // used when displaying a thread (for the backlink), and when moving/splitting a thread (for displaying the board tree)
    public BoardViewData(long index, String name)
    {
        this(index, name, 0, 0, false);
    }
}
