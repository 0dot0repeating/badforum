package com.jinotrain.badforum.db;

public class BoardPermissionState
{
    public final BoardPermission perm;
    public final PermissionState state;

    public BoardPermissionState(BoardPermission perm, PermissionState state)
    {
        this.perm  = perm;
        this.state = state;
    }
}
