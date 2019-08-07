package com.jinotrain.badforum.db.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

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

    private Date creationDate;
    private Date expireDate;


    private ForumSession() {}

    public ForumSession(ForumUser user)
    {
        this.user = user;
        this.creationDate = new Date();
        this.expireDate   = this.creationDate;
    }


    public String    getId()           { return id; }
    public ForumUser getUser()         { return user; }
    public Date      getCreationDate() { return creationDate; }
    public Date      getExpireDate()   { return expireDate; }

    public void refreshLastUseDate()
    {
        expireDate = new Date();
    }
}
