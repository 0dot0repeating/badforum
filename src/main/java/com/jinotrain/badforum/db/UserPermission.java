package com.jinotrain.badforum.db;

public enum UserPermission
{
    MANAGE_USERS,       // delete and/or modify any user and any role (indirectly gives all permissions)
    MANAGE_BOARDS,      // can create boards, delete boards, and modify board permissions (indirectly gives all board permissions)
    MANAGE_DETACHED,    // can moderate threads not attached to a board, and posts not attached to a thread
    ;
}
