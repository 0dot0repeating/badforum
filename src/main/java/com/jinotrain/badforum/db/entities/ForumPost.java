package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.time.Instant;

@Entity
public class ForumPost implements Comparable<ForumPost>
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected long id;

    @Column(nullable = false)
    protected String postText;

    @Column(nullable = false)
    protected Instant postTime;

    protected Instant lastEditTime;

    @ManyToOne
    @JoinColumn(name = "author_id")
    protected ForumUser author;

    @ManyToOne
    @JoinColumn(name = "thread_id")
    protected ForumThread thread;


    public ForumPost()
    {
        this("", null);
    }

    public ForumPost(String postText)
    {
        this(postText, null);
    }

    public ForumPost(String postText, ForumUser author)
    {
        this.postText     = postText;
        this.postTime = Instant.now();
        this.lastEditTime = null;
        this.author       = author;
    }


    public long getID()
    {
        return id;
    }

    public String getPostText()         { return postText; }
    public void   setPostText(String t) { postText = t; }

    public Instant getPostTime()          { return postTime; }
    public void    setPostTime(Instant t) { postTime = t; }

    public Instant getlastEditTime()          { return lastEditTime; }
    public void    setlastEditTime(Instant t) { lastEditTime = t; }

    public ForumUser getAuthor()            { return author; }
    public void      setAuthor(ForumUser a) { author = a; }

    public ForumThread getThread()            { return thread; }
    public void        setThread(ForumThread t) { thread = t; }


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
}
