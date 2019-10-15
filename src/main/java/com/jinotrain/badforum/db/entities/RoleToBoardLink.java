package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;

import javax.persistence.*;

@Entity
@Cacheable
@Table(name="forum_role_board_links")
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

    private byte canView;
    private byte canPost;
    private byte canModerate;

    @SuppressWarnings("unused")
    RoleToBoardLink() {}

    RoleToBoardLink(ForumRole role, ForumBoard board)
    {
        this.role     = role;
        this.board    = board;

        canView = 0;
        canPost = 0;
    }

    ForumRole  getRole()  { return role; }
    ForumBoard getBoard() { return board; }


    byte hasPermission(BoardPermission type)
    {
        switch (type)
        {
            case VIEW:     return canView;
            case POST:     return canPost;
            case MODERATE: return canModerate;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in RoleToBoardLink");

        }
    }


    void setPermission(BoardPermission type, byte state)
    {
        switch (type)
        {
            case VIEW:     canView     = state; break;
            case POST:     canPost     = state; break;
            case MODERATE: canModerate = state; break;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in RoleToBoardLink");
        }
    }
}
