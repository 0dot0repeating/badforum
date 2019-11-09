package com.jinotrain.badforum.data;

import java.util.List;

public class BoardViewData
{
    public long    index            = -1;
    public String  name             = null;
    public long    threadCount      = 0;
    public long    postCount        = 0;
    public long    totalThreadCount = 0;
    public long    totalPostCount   = 0;
    public boolean isRootBoard      = false;
    public boolean canManage        = false;
    public boolean canModerate      = false;
    public int[]   threadRange      = {0, 0};

    public List<BoardViewData>  childBoards = null;
    public List<ThreadViewData> threads     = null;

    // used when displaying a given board
    public BoardViewData(long index, String name, long threadCount, long postCount, boolean isRootBoard, int[] threadRange)
    {
        this.index       = index;
        this.name        = name;
        this.threadCount = threadCount;
        this.postCount   = postCount;
        this.isRootBoard = isRootBoard;
        this.threadRange = threadRange;
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
        this.index = index;
        this.name  = name;
    }
}
