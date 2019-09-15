package com.jinotrain.badforum.db.entities;

import javax.persistence.*;

@Entity
@Cacheable
@Table(name="forum_user_role_links")
class UserToRoleLink
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
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

    UserToRoleLink(ForumUser user, ForumRole role)
    {
        this.user = user;
        this.role = role;
    }

    long      getId()   { return id; }
    ForumUser getUser() { return user; }
    ForumRole getRole() { return role; }
}
