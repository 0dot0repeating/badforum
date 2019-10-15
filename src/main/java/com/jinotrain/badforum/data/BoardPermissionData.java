package com.jinotrain.badforum.data;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.BoardPermissionState;
import com.jinotrain.badforum.db.PermissionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BoardPermissionData
{
    public String roleName;
    public List<BoardPermissionState> permissions;


    public BoardPermissionData(String name)
    {
        this.roleName    = name;
        this.permissions = new ArrayList<>();
    }

    public BoardPermissionData(String name, Map<BoardPermission, PermissionState> permissions)
    {
        this.roleName    = name;
        this.permissions = new ArrayList<>();

        for (BoardPermission p: BoardPermission.values())
        {
            BoardPermissionState perm = new BoardPermissionState(p, permissions.getOrDefault(p, PermissionState.KEEP));
            this.permissions.add(perm);
        }
    }
}
