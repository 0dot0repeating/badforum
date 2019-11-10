package com.jinotrain.badforum.data;

public class UserSettingViewData
{
    public String   id;
    public String   name;
    public String   value;
    public String   inputType       = "text";
    public boolean  needsConfirm    = false;
    public boolean  readonly        = false;
    public String   description     = "";
    public String   message         = "";
    public boolean  error           = false;
    public String[] choices         = null;

    public UserSettingViewData(String id, String name, String value)
    {
        this.id    = id;
        this.name  = name;
        this.value = value;
    }
}
