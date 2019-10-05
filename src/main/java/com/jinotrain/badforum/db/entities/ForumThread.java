package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

@Entity
@Cacheable
@Table(name="forum_threads")
@SequenceGenerator(name="SEQ_THREADS")
@NamedEntityGraph(name="ForumThread.withPosts",
                    attributeNodes =
                    {
                        @NamedAttributeNode(value = "posts", subgraph = "withAuthor")
                    },

                    subgraphs =
                    {
                        @NamedSubgraph(name="withAuthor",
                        attributeNodes =
                        {
                            @NamedAttributeNode("author")
                        })
                    }
                 )
public class ForumThread
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SEQ_THREADS")
    protected Long id;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread")
    private Collection<ForumPost> posts;

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

    public ForumThread(String topic, Collection<ForumPost> posts)
    {
        this.topic        = topic;
        this.posts        = posts == null ? new HashSet<>() : new HashSet<>(posts);
        this.creationTime = Instant.now();
        this.lastUpdate   = this.creationTime;
    }

    public Long getId()        { return id; }
    public void setId(Long id) { this.id = id; }

    public Collection<ForumPost> getPosts() { return posts; }
    public void addPost(ForumPost post)     { posts.add(post); }
    public void removePost(ForumPost post)  { posts.remove(post); }

    public ForumBoard getBoard()                 { return board; }
    public void       setBoard(ForumBoard board) { this.board = board; }

    public String getTopic()             { return topic; }
    public void   setTopic(String topic) { this.topic = topic; }

    public Instant getCreationTime()                     { return creationTime; }
    public void    setCreationTime(Instant creationTime) { this.creationTime = creationTime; }

    public Instant getLastUpdate()                   { return lastUpdate; }
    public void    setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }
}
