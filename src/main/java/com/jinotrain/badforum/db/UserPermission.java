package com.jinotrain.badforum.db;

public enum UserPermission
{
    // note: in general, you can't mess with users with higher max priority than you, or the stuff they've made

    MANAGE_ROLES("Manage roles", PermissionState.KEEP),       // can modify roles with priority lower than your highest
    MANAGE_USERS("Manage users", PermissionState.KEEP),       // can change user settings and the roles they have
    MANAGE_BOARDS("Manage boards", PermissionState.KEEP),     // can create boards, delete boards, and manage permissions on boards
    MANAGE_DETACHED("Manage detached threads/posts", PermissionState.KEEP),    // can moderate threads not attached to a board, and posts not attached to a thread
    BAN_USERS("Ban users", PermissionState.KEEP),     // can ban users
    ;

    public final String label;
    public final PermissionState defaultState;

    UserPermission(String label, PermissionState defaultState)
    {
        this.label        = label;
        this.defaultState = defaultState;
    }
}
