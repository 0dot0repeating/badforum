package com.jinotrain.badforum.db.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;

@Entity
public class ForumSession
{
    @Id
    @GeneratedValue(generator="randomID")
    @GenericGenerator(name="randomID", strategy="com.jinotrain.badforum.db.RandomIDGenerator")
    private String id;

    @ManyToOne
    @JoinColumn(name="user_id")
    private ForumUser user;

    private Instant  creationTime;
    private Instant  lastUseTime;
    private Instant  expireTime;
    private Duration refreshDuration;


    private ForumSession() {}

    public ForumSession(ForumUser user)
    {
        this(user, Duration.ofHours(1));
    }

    public ForumSession(ForumUser user, Duration refreshDuration)
    {
        this.user = user;
        this.refreshDuration = refreshDuration;
        this.creationTime    = Instant.now();
        this.lastUseTime     = this.creationTime;
        this.expireTime      = this.creationTime.plus(this.refreshDuration);
    }


    public String    getId()           { return id; }
    public ForumUser getUser()         { return user; }
    public Instant   getCreationTime() { return creationTime; }
    public Instant   getLastUseTime()  { return lastUseTime; }
    public Instant   getExpireTime()   { return expireTime; }

    public Duration getRefreshDuration()           { return refreshDuration; }
    public void     setRefreshDuration(Duration t) { refreshDuration = t; }


    public void refreshExpireTime()
    {
        refreshExpireTime(refreshDuration);
    }

    public void refreshExpireTime(Duration delta)
    {
        lastUseTime = Instant.now();
        expireTime  = lastUseTime.plus(delta);
    }
}
