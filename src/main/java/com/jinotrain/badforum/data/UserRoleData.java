package com.jinotrain.badforum.data;

import java.util.ArrayList;
import java.util.List;

public class UserRoleData
{
    public String roleName;
    public boolean isAdmin   = false;
    public boolean isDefault = false;
    public boolean canModify = false;
    public Integer priority  = null;
    public List<UserPermissionStateData> permissions;

    // for <label>s in the HTML view
    public long viewIndex = 0;

    public UserRoleData(String name)
    {
        this.roleName    = name;
        this.permissions = new ArrayList<>();
    }

    public UserRoleData(String name, int priority, boolean isAdmin, boolean isDefault, boolean canModify, List<UserPermissionStateData> permissions, long viewIndex)
    {
        this.roleName    = name;
        this.priority    = priority;
        this.viewIndex   = viewIndex;
        this.isAdmin     = isAdmin;
        this.isDefault   = isDefault;
        this.canModify   = canModify;
        this.permissions = permissions;
    }
}
