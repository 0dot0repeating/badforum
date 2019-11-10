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

    private Integer pageSize = null;


    @SuppressWarnings("unused")
    ForumPreferences() {}

    public ForumPreferences(ForumUser user)
    {
        this.user = user;
    }

    public Long      getId()   { return id; }
    public ForumUser getUser() { return user; }

    public static int getPageSize(ForumPreferences pref)
    {
        if (pref == null || pref.pageSize == null) { return 30; }
        return pref.pageSize;
    }

    public void setPageSize(int pageSize)
    {
        this.pageSize = pageSize;
    }
}