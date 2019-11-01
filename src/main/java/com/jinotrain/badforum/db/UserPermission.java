package com.jinotrain.badforum.db;

public enum UserPermission
{
    MANAGE_ROLES("Manage roles"),       // can modify roles (indirectly gives all permissions)
    MANAGE_USERS("Manage users"),       // can modify the settings of any user who doesn't outrank them
    MANAGE_BOARDS("Manage boards"),      // can create boards, delete boards, and modify board permissions (indirectly gives all board permissions)
    MANAGE_DETACHED("Manage detached threads/posts"),    // can moderate threads not attached to a board, and posts not attached to a thread
    BAN_USERS("Ban users"),     // can ban users if none of the bannee's roles are higher priority than the banner's
    ;

    public final String label;

    UserPermission(String label)
    {
        this.label = label;
    }
}
