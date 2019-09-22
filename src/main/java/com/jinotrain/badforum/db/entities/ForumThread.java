package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Cacheable
@Table(name="forum_threads")
public class ForumThread
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected Long id;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ForumPost> posts;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private ForumBoard board;

    @Column(nullable = false)
    private String topic;

    private Instant creationTime;

    private Instant lastUpdate;


    @SuppressWarnings("unused")
    ForumThread() {}

    public ForumThread(String topic)
    {
        this(topic, null);
    }

    public ForumThread(String topic, List<ForumPost> posts)
    {
        this.topic        = topic;
        this.posts        = posts == null ? new ArrayList<>() : new ArrayList<>(posts);
        this.creationTime = Instant.now();
        this.lastUpdate   = this.creationTime;
    }
}
