package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Cacheable
@Table(name="forum_threads")
@NamedQuery(name="ForumThread.getPostCount", query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.id = :threadID AND p.split = false AND p.deleted = false")
public class ForumThread
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private long index;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "thread", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<ForumPost> posts;

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

    private boolean deleted;

    private boolean moved;
    private Long    moveIndex;


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


    public static ForumThread transferPostsToNewThread(long index, ForumThread other, ForumBoard board)
    {
        ForumThread thread = new ForumThread();

        thread.index        = index;
        thread.topic        = other.topic;
        thread.board        = other.board;
        thread.author       = other.author;
        thread.posts        = new HashSet<>(other.posts);
        thread.creationTime = other.creationTime;
        thread.lastUpdate   = Instant.now();
        thread.board        = board;

        for (ForumPost p: thread.posts) { p.setThread(thread); }
        other.posts.clear();

        return thread;
    }


    public Long getID()     { return id; }
    public long getIndex()  { return index; }

    public Collection<ForumPost> getPosts() { return posts; }

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

    public boolean isDeleted() { return deleted; }

    public boolean wasMoved()     { return moved; }
    public Long    getMoveIndex() { return moveIndex; }


    public void setThreadMoved(long moveIndex)
    {
        this.moved = true;
        this.moveIndex = moveIndex;
        this.lastUpdate = Instant.now();
        this.topic = "[moved]";
    }


    public void deleteContents(boolean deletePosts, ForumUser user)
    {
        this.topic   = "[deleted]";
        this.board   = null;
        this.deleted = true;

        for (ForumPost post: posts)
        {
            if (deletePosts)
            {
                ForumUser postAuthor = post.getAuthor();

                if (ForumUser.userOutranksOrIs(user, postAuthor))
                {
                    post.deleteContents();
                }
            }

            post.setThread(null);
        }
    }
}
