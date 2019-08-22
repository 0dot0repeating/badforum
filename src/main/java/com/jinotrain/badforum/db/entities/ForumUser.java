package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.time.Instant;
import java.util.Iterator;

@Entity
@Cacheable
public class ForumUser
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passhash;

    private String email;

    private Boolean enabled;

    private Instant creationTime;
    private Instant lastLoginTime;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "user")
    private Collection<UserToRoleLink> roleLinks;


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
        this.lastLoginTime = Instant.MIN;
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


    public boolean hasRole(ForumRole role)
    {
        Long roleID = role.getId();

        for (UserToRoleLink link: roleLinks)
        {
            ForumRole linkRole = link.getRole();
            if (role == linkRole || roleID.equals(linkRole.getId())) { return true; }
        }

        return false;
    }


    public void addRole(ForumRole role)
    {
        if (!hasRole(role))
        {
            UserToRoleLink newLink = new UserToRoleLink(this, role);
            roleLinks.add(newLink);
        }
    }


    public void removeRole(ForumRole role)
    {
        Long roleID = role.getId();

        Iterator<UserToRoleLink> iter = roleLinks.iterator();

        while (iter.hasNext())
        {
            UserToRoleLink link = iter.next();
            ForumRole linkRole = link.getRole();

            if (role == linkRole || roleID.equals(linkRole.getId()))
            {
                iter.remove();
                return;
            }
        }
    }
}
