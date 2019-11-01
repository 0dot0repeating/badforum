package com.jinotrain.badforum.data;

import java.time.Instant;

public class PostViewData
{
    public long index;
    public String text;
    public UserViewData author;
    public Instant postTime;
    public Instant lastEditTime;
    public boolean deleted;
    public boolean viewerIsAuthor;
    public boolean userBanned;
    public String  banReason;
    public boolean canModerate;
    public boolean canBan;


    public PostViewData(long index, String text, UserViewData author, Instant postTime, Instant lastEditTime, boolean deleted, boolean banned, String banReason)
    {
        this.index          = index;
        this.text           = text;
        this.author         = author;
        this.postTime       = postTime;
        this.lastEditTime   = lastEditTime;
        this.deleted        = deleted;
        this.userBanned     = banned;
        this.viewerIsAuthor = false;
        this.banReason      = banReason;
    }
}
