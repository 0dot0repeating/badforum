package com.jinotrain.badforum.data;

public class UserSettingViewData
{
    public String  id;
    public String  name;
    public String  value;
    public String  inputType;
    public boolean needsConfirm;
    public boolean readonly;
    public String  description;
    public String  message;
    public boolean error;

    public UserSettingViewData(String id, String name, String value)
    {
        this.id             = id;
        this.name           = name;
        this.value          = value;
        this.inputType      = "text";
        this.needsConfirm   = false;
        this.readonly       = false;
        this.description    = "";
        this.message        = "";
        this.error          = false;
    }

}
