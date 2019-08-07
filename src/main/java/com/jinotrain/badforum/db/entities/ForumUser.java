package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.time.Instant;

@Entity
public class ForumUser
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected long id;

    @Column(unique = true, nullable = false)
    protected String username;

    @Column(nullable = false)
    protected String passhash;

    protected String email;

    protected Boolean enabled;

    protected Instant creationTime;
    protected Instant lastLoginTime;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "user")
    protected Collection<UserToRoleLink> roleLinks;


    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { passhash = s; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { email = e; }

    public Boolean getEnabled()       { return enabled; }
    public void setEnabled(Boolean e) { enabled = e; }

    public Instant getCreationTime() { return creationTime; }

    public Instant getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Instant d) { lastLoginTime = d; }


    private ForumUser() {}

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
        this.creationTime  = Instant.now();
        this.lastLoginTime = Instant.now();
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
