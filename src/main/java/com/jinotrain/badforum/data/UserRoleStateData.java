package com.jinotrain.badforum.data;

public class UserRoleStateData
{
    public String  roleName;
    public boolean present;
    public boolean isAdmin;
    public long viewIndex;

    public UserRoleStateData(String roleName, boolean present, boolean isAdmin, int viewIndex)
    {
        this.roleName  = roleName;
        this.present   = present;
        this.viewIndex = viewIndex;
        this.isAdmin   = isAdmin;
    }
}
