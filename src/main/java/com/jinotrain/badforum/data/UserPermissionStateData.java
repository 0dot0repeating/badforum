package com.jinotrain.badforum.data;

import com.jinotrain.badforum.db.PermissionState;
import com.jinotrain.badforum.db.UserPermission;

public class UserPermissionStateData
{
    public final UserPermission perm;
    public final PermissionState state;

    public UserPermissionStateData(UserPermission perm, PermissionState state)
    {
        this.perm  = perm;
        this.state = state;
    }
}
