package com.jinotrain.badforum.db.entities;

import javax.persistence.*;

@Entity
public class RoleToBoardLink
{
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private ForumRole role;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private ForumBoard board;

    private boolean canView;
    private boolean canPost;

    public RoleToBoardLink()
    {
        this(null, null);
    }

    public RoleToBoardLink(ForumRole role, ForumBoard board)
    {
        this.role  = role;
        this.board = board;

        canView = false;
        canPost = false;
    }

    public ForumRole  getRole()  { return role; }
    public ForumBoard getBoard() { return board; }

    public boolean getCanView() { return canView; }
    public boolean getCanPost() { return canPost; }
}
