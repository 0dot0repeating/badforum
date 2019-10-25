package com.jinotrain.badforum.db;

public enum UserPermission
{
    MANAGE_USERS("Manage users/roles"),       // delete and/or modify any user and any role (indirectly gives all permissions)
    MANAGE_BOARDS("Manage boards"),      // can create boards, delete boards, and modify board permissions (indirectly gives all board permissions)
    MANAGE_DETACHED("Manage detached threads/posts"),    // can moderate threads not attached to a board, and posts not attached to a thread
    ;

    public final String label;

    UserPermission(String label)
    {
        this.label = label;
    }
}
