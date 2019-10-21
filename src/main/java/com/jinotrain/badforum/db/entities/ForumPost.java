package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Cacheable
@Table(name="forum_posts")
public class ForumPost implements Comparable<ForumPost>
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private long index;

    @Column(nullable = false)
    private String postText;

    @Column(nullable = false)
    private Instant postTime;

    private Instant lastEditTime = null;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private ForumUser author;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "thread_id")
    private ForumThread thread;

    private boolean deleted = false;


    @SuppressWarnings("unused")
    ForumPost() {}

    public ForumPost(long index, String postText)
    {
        this(index, postText, null);
    }

    public ForumPost(long index, String postText, ForumUser author)
    {
        this.index    = index;
        this.postText = postText;
        this.postTime = Instant.now();
        this.author   = author;
    }


    public Long getID()     { return id; }
    public long getIndex()  { return index; }

    public String getPostText()               { return postText; }
    public void   setPostText(String t)       { postText = t; }

    public Instant getPostTime()              { return postTime; }
    public void    setPostTime(Instant t)     { postTime = t; }

    public Instant getlastEditTime()          { return lastEditTime; }
    public void    setlastEditTime(Instant t) { lastEditTime = t; }

    public ForumUser getAuthor()              { return author; }
    public void      setAuthor(ForumUser a)   { author = a; }

    public ForumThread getThread()            { return thread; }
    public void        setThread(ForumThread t) { thread = t; }

    public boolean isDeleted() { return deleted; }


    @Override
    public int compareTo(ForumPost o)
    {
         int ret = postTime.compareTo(o.postTime);
         if (ret != 0) { return ret; }

         if (author   == null) { return -1; }
         if (o.author == null) { return  1; }

         ret = author.getUsername().compareTo(o.author.getUsername());
         if (ret != 0) { return ret; }

         return postText.compareTo(o.postText);
    }


    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ForumPost)) { return false; }
        return compareTo((ForumPost)o) == 0;
    }


    @Override
    public int hashCode()
    {
        return postText.hashCode();
    }


    public void deleteContents()
    {
        deleted = true;

        author = null;
        postText = "[deleted]";
        lastEditTime = Instant.now();
    }
}
