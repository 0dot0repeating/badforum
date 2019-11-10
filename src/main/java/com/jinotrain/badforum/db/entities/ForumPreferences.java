package com.jinotrain.badforum.db.entities;

import javax.persistence.*;

@Entity
@Cacheable
@Table(name="forum_preferences")
public class ForumPreferences
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private ForumUser user;

    private Integer threadPageSize = null;
    private Integer boardPageSize  = null;


    @SuppressWarnings("unused")
    ForumPreferences() {}

    public ForumPreferences(ForumUser user)
    {
        this.user = user;
    }

    public Long      getId()   { return id; }
    public ForumUser getUser() { return user; }

    public static int getBoardPageSize(ForumPreferences pref)
    {
        if (pref == null || pref.boardPageSize == null) { return 30; }
        return pref.boardPageSize;
    }

    public static int getThreadPageSize(ForumPreferences pref)
    {
        if (pref == null || pref.threadPageSize == null) { return 30; }
        return pref.threadPageSize;
    }

    public void setBoardPageSize(int pageSize)
    {
        this.boardPageSize = pageSize;
    }

    public void setThreadPageSize(int pageSize)
    {
        this.threadPageSize = pageSize;
    }
}