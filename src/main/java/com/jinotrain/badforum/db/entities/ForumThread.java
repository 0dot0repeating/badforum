package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;

@Entity
@Cacheable
@Table(name="forum_threads")
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
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private long index;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<ForumPost> posts;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private ForumBoard board;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private ForumUser author;

    @Column(nullable = false)
    private String topic;

    private Instant creationTime;
    private Instant lastUpdate;


    @SuppressWarnings("unused")
    ForumThread() {}

    public ForumThread(long index, String topic, ForumBoard board, ForumUser author)
    {
        this.index        = index;
        this.topic        = topic;
        this.board        = board;
        this.author       = author;
        this.posts        = new HashSet<>();
        this.creationTime = Instant.now();
        this.lastUpdate   = this.creationTime;
    }

    public Long getID()     { return id; }
    public long getIndex()  { return index; }

    public Collection<ForumPost> getPosts()      { return posts; }
    public void addPost(ForumPost post)          { posts.add(post); }
    public void removePost(ForumPost post)       { posts.remove(post); }

    public ForumBoard getBoard()                 { return board; }
    public void       setBoard(ForumBoard board) { this.board = board; }

    public ForumUser  getAuthor()                   { return author; }
    public void       setAuthor(ForumUser author)   { this.author = author; }

    public String getTopic()                             { return topic; }
    public void   setTopic(String topic)                 { this.topic = topic; }

    public Instant getCreationTime()                     { return creationTime; }
    public void    setCreationTime(Instant creationTime) { this.creationTime = creationTime; }

    public Instant getLastUpdate()                       { return lastUpdate; }
    public void    setLastUpdate(Instant lastUpdate)     { this.lastUpdate = lastUpdate; }
}
