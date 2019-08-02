package com.jinotrain.badforum.db.entities;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Collection;
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

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "user")
    protected Collection<UserToRoleLink> roleLinks;


    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { this.passhash = s; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { this.email = e; }

    public Boolean getEnabled()       { return enabled; }
    public void setEnabled(Boolean e) { this.enabled = e; }


    protected ForumUser()
    {
        this("user", "{noop}password");
    }


    public ForumUser(String name, String passhash)
    {
        this.username  = name;
        this.passhash  = passhash;
        this.roleLinks = new HashSet<>();
        this.enabled   = true;
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
