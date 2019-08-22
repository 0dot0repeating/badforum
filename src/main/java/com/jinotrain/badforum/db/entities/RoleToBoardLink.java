package com.jinotrain.badforum.db.entities;

import javax.persistence.*;

@Entity
@Cacheable
class RoleToBoardLink
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private ForumRole role;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private ForumBoard board;

    private boolean canView;
    private boolean canPost;

    RoleToBoardLink() { this(null, null); }

    RoleToBoardLink(ForumRole role, ForumBoard board)
    {
        this.role  = role;
        this.board = board;

        canView = false;
        canPost = false;
    }

    ForumRole  getRole()  { return role; }
    ForumBoard getBoard() { return board; }

    boolean hasPermission(ForumRole.Permission type)
    {
        switch (type)
        {
            case VIEW: return canView;
            case POST: return canPost;
            default: return false;
        }
    }


    void setPermission(ForumRole.Permission type, boolean onOff)
    {
        switch (type)
        {
            case VIEW: canView = onOff; break;
            case POST: canPost = onOff; break;
        }
    }
}
