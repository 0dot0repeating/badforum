package com.jinotrain.badforum.db;

public enum PermissionState
{
    OFF,    // deny this permission, even if lower roles grant it
    KEEP,   // grant permission if lower role grants it, otherwise deny
    ON,     // grant permission
}
