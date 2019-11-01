package com.jinotrain.badforum.data;

import java.util.List;

public class UserViewData
{
    public String  username  = null;
    public boolean isBanned = false;
    public List<UserRoleStateData> roles = null;

    public UserViewData() {}

    public UserViewData(String username, boolean isBanned)
    {
        this.username = username;
        this.isBanned = isBanned;
    }
}
