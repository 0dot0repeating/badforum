package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class ForumThread
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected long id;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    protected List<ForumPost> posts;

    @ManyToOne
    @JoinColumn(name = "board_id")
    protected ForumBoard board;

    @Column(nullable = false)
    protected String topic;

    protected Date creationDate;

    protected Date lastUpdate;


    public ForumThread()
    {
        this("", null);
    }

    public ForumThread(String topic, List<ForumPost> posts)
    {
        this.topic        = topic;
        this.posts        = posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        this.creationDate = new Date();
        this.lastUpdate   = this.creationDate;
    }
}
