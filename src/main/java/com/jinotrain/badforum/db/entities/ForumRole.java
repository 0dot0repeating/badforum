package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;

@Entity
public class ForumRole
{
    @Id
    @GeneratedValue
    private long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "roles")
    private Collection<ForumUser> users;

    @Enumerated(EnumType.ORDINAL)
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(nullable = false)
    private Collection<ForumPrivilegeType> privileges;

    private ForumRole()
    {
        this("");
    }

    public ForumRole(String name)
    {
        this.id   = -1;
        this.name = name;
        this.privileges = new HashSet<>();
    }

    public String getName()         { return name; }
    public void   setName(String n) { name = n; }


    public Boolean hasPrivilege(ForumPrivilegeType privilege)
    {
        if (privileges == null) { return false; }
        return privileges.contains(privilege);
    }


    public void modifyPrivilege(ForumPrivilegeType privilege, Boolean onOff)
    {
        if (privileges == null) { privileges = new HashSet<>(); }

        if (onOff && !privileges.contains(privilege))
        {
            privileges.add(privilege);
        }

        if (!onOff && privileges.contains(privilege))
        {
            privileges.remove(privilege);
        }
    }
}
