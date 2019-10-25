package com.jinotrain.badforum.data;

import com.jinotrain.badforum.db.PermissionState;
import com.jinotrain.badforum.db.BoardPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardRoleData
{
    public String roleName;
    public boolean isAdmin;
    public List<BoardPermissionStateData> permissions;


    public BoardRoleData(String name)
    {
        this.roleName    = name;
        this.permissions = new ArrayList<>();
    }

    public BoardRoleData(String name, boolean isAdmin, Map<BoardPermission, PermissionState> permissions)
    {
        this.roleName    = name;
        this.isAdmin     = isAdmin;
        this.permissions = new ArrayList<>();

        for (BoardPermission p: BoardPermission.values())
        {
            BoardPermissionStateData perm = new BoardPermissionStateData(p, permissions.getOrDefault(p, PermissionState.KEEP));
            this.permissions.add(perm);
        }
    }
}
