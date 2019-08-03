package com.jinotrain.badforum.db.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

@Entity
public class ForumUser
{
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    protected UUID id;

    @Column(unique = true, nullable = false)
    protected final String username;

    @Column(nullable = false)
    protected String passhash;

    protected String email;

    protected Boolean enabled;

    protected Date creationDate;
    protected Date lastLoginDate;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "user")
    protected Collection<UserToRoleLink> roleLinks;


    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { passhash = s; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { email = e; }

    public Boolean getEnabled()       { return enabled; }
    public void setEnabled(Boolean e) { enabled = e; }

    public Date getCreationDate() { return creationDate; }

    public Date getLastLoginDate() { return lastLoginDate; }
    public void setLastLoginDate(Date d) { lastLoginDate = d; }


    protected ForumUser()
    {
        this("user", "{noop}password", null);
    }


    public ForumUser(String name, String passhash)
    {
        this(name, passhash, null);
    }


    public ForumUser(String name, String passhash, String email)
    {
        this.username      = name;
        this.passhash      = passhash;
        this.email         = email;
        this.roleLinks     = new HashSet<>();
        this.creationDate  = new Date();
        this.lastLoginDate = new Date();
        this.enabled       = true;
    }


    public Collection<ForumRole> getRoles()
    {
        Collection<ForumRole> roles = new HashSet<>();

        for (UserToRoleLink link: roleLinks)
        {
            roles.add(link.getRole());
        }

        return roles;
    }


    public boolean canViewBoard(ForumBoard board)
    {
        for (UserToRoleLink roleLink: roleLinks)
        {
            ForumRole role = roleLink.getRole();
            if (role.canViewBoard(board)) { return true; }
        }

        return false;
    }


    public boolean canPostInBoard(ForumBoard board)
    {
        for (UserToRoleLink roleLink: roleLinks)
        {
            ForumRole role = roleLink.getRole();
            if (role.canPostInBoard(board)) { return true; }
        }

        return false;
    }
}
