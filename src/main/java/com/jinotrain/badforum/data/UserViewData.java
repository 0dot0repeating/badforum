package com.jinotrain.badforum.data;

import java.util.List;

public class UserViewData
{
    public String username;
    public boolean isBanned;
    public List<UserRoleStateData> roles;

    public UserViewData(String username, boolean isBanned)
    {
        this.username = username;
        this.isBanned = isBanned;
        this.roles    = null;
    }

    public UserViewData(String username, List<UserRoleStateData> roles)
    {
        this.username = username;
        this.roles    = roles;
    }
}
