package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.ForumPermission;
import com.jinotrain.badforum.db.PermissionState;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;


@Entity
@Cacheable
@Table(name="forum_roles")
@SequenceGenerator(name="SEQ_ROLES")
public class ForumRole
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SEQ_ROLES")
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "role")
    private Collection<UserToRoleLink> users;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "role")
    private Collection<RoleToBoardLink> accessBoards;

    private int priority;

    private boolean admin;
    private boolean defaultRole;

    private byte canManageUsers;


    @SuppressWarnings("unused")
    ForumRole() {}

    public ForumRole(String name, int priority)
    {
        this.name = name;
        this.admin = false;
        this.accessBoards = new ArrayList<>();
        this.priority = priority;
    }


    public Long getId() { return id; }

    public String getName()         { return name; }
    public void   setName(String n) { name = n; }

    public int  getPriority()             { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isDefaultRole() { return defaultRole; }
    public void    setDefaultRole(boolean onOff) { defaultRole = onOff; }

    public boolean isAdmin() { return admin; }
    public void    setAdmin(boolean onOff) { admin = onOff; }



    public PermissionState hasPermission(ForumPermission type)
    {
        if (admin) { return PermissionState.ON; }

        byte state = 0;

        switch (type)
        {
            case MANAGE_USERS: state = canManageUsers; break;
            default: throw new UnsupportedOperationException("User-level permission " + type.name() + " not implemented in ForumRole");
        }

        switch (state)
        {
            case -1: return PermissionState.OFF;
            default: return PermissionState.KEEP;
            case  1: return PermissionState.ON;
        }
    }


    public void setPermission(ForumPermission type, PermissionState state)
    {
        byte internalState;

        switch (state)
        {
            case OFF:  internalState = -1; break;
            default:   internalState =  0; break;
            case ON:   internalState =  1; break;
        }

        switch (type)
        {
            case MANAGE_USERS: canManageUsers = internalState; break;
            default: throw new UnsupportedOperationException("User-level permission " + type.name() + " not implemented in ForumRole");
        }
    }



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


    public PermissionState getBoardPermission(ForumBoard board, BoardPermission type)
    {
        if (admin) { return PermissionState.ON; }

        RoleToBoardLink link = findBoardLink(board);
        if (link == null) { return PermissionState.KEEP; }

        byte state = link.hasPermission(type);

        switch (state)
        {
            case -1: return PermissionState.OFF;
            default: return PermissionState.KEEP;
            case  1: return PermissionState.ON;
        }
    }


    public void setBoardPermission(ForumBoard board, BoardPermission type, PermissionState state)
    {
        RoleToBoardLink link = findBoardLink(board);

        if (link == null)
        {
            link = new RoleToBoardLink(this, board);
            accessBoards.add(link);
        }

        switch (state)
        {
            case OFF:  link.setPermission(type, (byte)-1); break;
            case KEEP: link.setPermission(type, (byte)0);  break;
            case ON:   link.setPermission(type, (byte)1);  break;
        }
    }


    public void clearBoardPermissions(ForumBoard board)
    {
        RoleToBoardLink link = findBoardLink(board);

        if (link != null)
        {
            accessBoards.remove(link);
        }
    }
}
