package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;


@Entity
@Cacheable
public class ForumRole
{
    public enum Permission
    {
        VIEW,
        POST,
    }

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "role")
    private Collection<UserToRoleLink> users;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "role")
    private Collection<RoleToBoardLink> accessBoards;

    private boolean admin;


    public ForumRole() { this(""); }

    public ForumRole(String name)
    {
        this.name = name;
        this.accessBoards   = new ArrayList<>();
        this.admin = false;
    }


    public Long getId() { return id; }

    public String getName()         { return name; }
    public void   setName(String n) { name = n; }

    public boolean isAdmin() { return admin; }
    public void    setAdmin(boolean onOff) { admin = onOff; }


    private RoleToBoardLink findBoardLink(ForumBoard board)
    {
        Long boardID = board.getId();

        for (RoleToBoardLink link: accessBoards)
        {
            ForumBoard linkBoard = link.getBoard();

            if (board == linkBoard || boardID.equals(linkBoard.getId()))
            {
                return link;
            }
        }

        return null;
    }


    public boolean hasPermission(ForumBoard board, Permission type)
    {
        RoleToBoardLink link = findBoardLink(board);
        if (link == null) { return false; }
        return link.hasPermission(type);
    }


    public void setPermission(ForumBoard board, Permission type, boolean onOff)
    {
        RoleToBoardLink link = findBoardLink(board);

        if (link == null)
        {
            link = new RoleToBoardLink(this, board);
            accessBoards.add(link);
        }

        link.setPermission(type, onOff);
    }


    public void clearPermissions(ForumBoard board)
    {
        RoleToBoardLink link = findBoardLink(board);

        if (link != null)
        {
            accessBoards.remove(link);
        }
    }
}
