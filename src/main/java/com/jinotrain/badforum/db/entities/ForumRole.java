package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.PermissionState;

import javax.persistence.*;
import java.util.*;

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
    private Set<UserToRoleLink> users;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "role")
    private Set<RoleToBoardLink> accessBoards;

    private int priority;

    private boolean admin;
    private boolean defaultRole;

    private byte canManageUsers;
    private byte canManageRoles;
    private byte canManageBoards;
    private byte canManageDetached;
    private byte canBanUsers;


    @SuppressWarnings("unused")
    ForumRole() {}

    public ForumRole(String name, int priority)
    {
        this.name = name;
        this.admin = false;
        this.accessBoards = new HashSet<>();
        this.priority = priority;

        for (UserPermission p: UserPermission.values())
        {
            setPermission(p, p.defaultState);
        }
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



    public PermissionState getPermission(UserPermission type)
    {
        if (admin) { return PermissionState.ON; }

        byte state;

        switch (type)
        {
            case MANAGE_USERS:    state = canManageUsers;  break;
            case MANAGE_ROLES:    state = canManageRoles;  break;
            case MANAGE_BOARDS:   state = canManageBoards; break;
            case MANAGE_DETACHED: state = canManageDetached; break;
            case BAN_USERS:       state = canBanUsers; break;
            default: throw new UnsupportedOperationException("User-level permission " + type.name() + " not implemented in ForumRole");
        }

        switch (state)
        {
            case -1: return PermissionState.OFF;
            default: return PermissionState.KEEP;
            case  1: return PermissionState.ON;
        }
    }


    public void setPermission(UserPermission type, PermissionState state)
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
            case MANAGE_USERS:    canManageUsers    = internalState; break;
            case MANAGE_ROLES:    canManageRoles    = internalState; break;
            case MANAGE_BOARDS:   canManageBoards   = internalState; break;
            case MANAGE_DETACHED: canManageDetached = internalState; break;
            case BAN_USERS:       canBanUsers       = internalState; break;
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


    public Map<BoardPermission, PermissionState> getBoardPermissions(ForumBoard board)
    {
        Map<BoardPermission, PermissionState> ret = new HashMap<>();

        if (admin)
        {
            for (BoardPermission p: BoardPermission.values()) { ret.put(p, PermissionState.ON); }
            return ret;
        }

        RoleToBoardLink link = findBoardLink(board);

        if (link == null)
        {
            for (BoardPermission p: BoardPermission.values()) { ret.put(p, PermissionState.KEEP); }
            return ret;
        }

        for (BoardPermission p: BoardPermission.values())
        {
            byte state = link.hasPermission(p);

            switch (state)
            {
                case -1: ret.put(p, PermissionState.OFF);   break;
                default: ret.put(p, PermissionState.KEEP);  break;
                case  1: ret.put(p, PermissionState.ON);    break;
            }
        }

        return ret;
    }
}
