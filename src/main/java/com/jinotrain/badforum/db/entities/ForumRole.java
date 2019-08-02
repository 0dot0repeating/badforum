package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Entity
public class ForumRole
{
    protected class PermissionSet
    {
        public boolean canView;
        public boolean canPost;
    }


    @Id
    @GeneratedValue
    protected Long id;

    @Column(unique = true, nullable = false)
    protected String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "role")
    protected Collection<UserToRoleLink> users;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "role")
    protected Collection<RoleToBoardLink> accessBoards;
    protected transient Map<ForumBoard, PermissionSet> permissionSets;


    public ForumRole()
    {
        this("");
    }

    public ForumRole(String name)
    {
        this.name = name;
    }

    public String getName()         { return name; }
    public void   setName(String n) { name = n; }


    private void syncPermissionSets()
    {
        if (org.hibernate.Hibernate.isInitialized(accessBoards)) { return; }

        permissionSets = new HashMap<>();

        for (RoleToBoardLink link: accessBoards)
        {
            ForumBoard board = link.getBoard();
            PermissionSet perms = permissionSets.get(board);

            // realistically there should only be one role-board pairing per role and board,
            //  but dumb things happen sometimes
            if (perms == null)
            {
                perms = new PermissionSet();
                permissionSets.put(board, perms);
            }

            perms.canView = perms.canView || link.getCanView();
            perms.canPost = perms.canPost || link.getCanPost();
        }
    }


    public boolean canViewBoard(ForumBoard board)
    {
        syncPermissionSets();

        PermissionSet perms = permissionSets.get(board);
        if (perms != null) { return perms.canView; }
        return false;
    }


    public boolean canPostInBoard(ForumBoard board)
    {
        syncPermissionSets();

        PermissionSet perms = permissionSets.get(board);
        if (perms != null) { return perms.canPost; }
        return false;
    }
}
