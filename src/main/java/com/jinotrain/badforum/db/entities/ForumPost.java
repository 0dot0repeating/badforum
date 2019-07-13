package com.jinotrain.badforum.db.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ForumPost
{
    @Id
    @GeneratedValue
    private long id;

    private long authorID;
    private String postText;

    public ForumPost()
    {
        this(-1, "");
    }

    public ForumPost(long authorID, String postText)
    {
        this.authorID = authorID;
        this.postText = postText;
    }


    public long getID()
    {
        return id;
    }


    public long getAuthorID()
    {
        return authorID;
    }


    public String getPostText()
    {
        return postText;
    }
}
