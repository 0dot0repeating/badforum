package com.jinotrain.badforum.db.entities;

import javax.persistence.*;

@Entity
public class UserToRoleLink
{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private ForumUser user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private ForumRole role;

    public UserToRoleLink()
    {
        this(null, null);
    }

    public UserToRoleLink(ForumUser user, ForumRole role)
    {
        this.user = user;
        this.role = role;
    }

    public ForumUser getUser() { return user; }
    public ForumRole getRole() { return role; }
}
