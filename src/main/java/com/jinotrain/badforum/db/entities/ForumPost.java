package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Date;

@Entity
public class ForumPost implements Comparable<ForumPost>
{
    @Id
    @GeneratedValue
    protected Long id;

    @Column(nullable = false)
    protected String postText;

    @Column(nullable = false)
    protected Date postDate;

    protected Date lastEditDate;

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
        this.postDate     = new Date();
        this.lastEditDate = null;
        this.author       = author;
    }


    public long getID()
    {
        return id;
    }

    public String getPostText()         { return postText; }
    public void   setPostText(String t) { postText = t; }

    public Date getPostDate()       { return postDate; }
    public void setPostDate(Date d) { postDate = d; }

    public Date getlastEditDate()       { return lastEditDate; }
    public void setlastEditDate(Date d) { lastEditDate = d; }

    public ForumUser getAuthor()            { return author; }
    public void      setAuthor(ForumUser a) { author = a; }

    public ForumThread getThread()            { return thread; }
    public void        setThread(ForumThread t) { thread = t; }


    @Override
    public int compareTo(ForumPost o)
    {
         int ret = postDate.compareTo(o.postDate);
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
