package com.jinotrain.badforum.data;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.PermissionState;

public class BoardPermissionStateData
{
    public final BoardPermission perm;
    public final PermissionState state;

    public BoardPermissionStateData(BoardPermission perm, PermissionState state)
    {
        this.perm  = perm;
        this.state = state;
    }
}
