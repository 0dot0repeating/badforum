package com.jinotrain.badforum.data;

public class PreAdminKey
{
    private String key;
    private Long adminRoleID;

    public PreAdminKey()
    {
        key = null;
        adminRoleID = null;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }


    public Long getAdminRoleID() { return adminRoleID; }
    public void setAdminRoleID(Long roleID) { adminRoleID = roleID; }
}
