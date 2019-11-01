package com.jinotrain.badforum.data;

public class UserRoleStateData
{
    public String  roleName;
    public boolean present;
    public boolean isAdmin;
    public boolean canGrant;
    public long viewIndex;

    public UserRoleStateData(String roleName, boolean present, boolean isAdmin, boolean canGrant, int viewIndex)
    {
        this.roleName  = roleName;
        this.present   = present;
        this.viewIndex = viewIndex;
        this.canGrant  = canGrant;
        this.isAdmin   = isAdmin;
    }
}
