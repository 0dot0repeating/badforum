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
    private final UUID id;

    @Column(unique = true, nullable = false)
    private final String username;

    @Column(nullable = false)
    private String passhash;

    private String email;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
                joinColumns = @JoinColumn(name="user_id"),
                inverseJoinColumns = @JoinColumn(name="role_id"))
    private Collection<ForumRole> roles;


    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { this.passhash = passhash; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { this.email = email; }


    private ForumUser()
    {
        this("user", "{noop}password");
    }


    public ForumUser(String name, String passhash)
    {
        this.id = UUID.randomUUID();
        this.username = name;
        this.passhash = passhash;
        this.roles    = new HashSet<>();
    }


    public Boolean hasRole(ForumRole role)
    {
        return roles.contains(role);
    }


    public void addRole(ForumRole role)
    {
        if (!hasRole(role)) { roles.add(role); }
    }


    public void removeRole(ForumRole role)
    {
        roles.remove(role);
    }


    public Boolean hasPrivilege(ForumPrivilegeType privilege)
    {
        for (ForumRole role: roles)
        {
            if (role.hasPrivilege(privilege)) { return true; }
        }

        return false;
    }
}
