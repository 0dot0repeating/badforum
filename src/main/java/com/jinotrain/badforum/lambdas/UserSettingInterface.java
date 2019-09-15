package com.jinotrain.badforum.lambdas;

import com.jinotrain.badforum.components.passwords.ForumPasswordService;
import com.jinotrain.badforum.db.entities.ForumUser;

import java.util.Map;

public interface UserSettingInterface
{
    String get(ForumUser user);
    String name();
    String id();

    default String  inputType()    { return "text"; }
    default String  description()  { return ""; }
    default boolean readonly()     { return false; }
    default boolean needsConfirm() { return false; }

    default boolean needsPasswordHasher()  { return false; }

    default boolean set(ForumUser user, Map<String, String[]> params) throws IllegalArgumentException
    {
        return false;
    }

    default boolean setWithHasher(ForumUser user, Map<String, String[]> params, ForumPasswordService hasher) throws IllegalArgumentException
    {
        return false;
    }


    default String getParam(Map<String, String[]> params, String key)
    {
        String[] val = params.get(key);
        if (val == null) { return null; }
        return val[0];
    }

    default boolean needsUpdate(ForumUser user, Map<String, String[]> params)
    {
        String paramVal = getParam(params, id());
        String myVal    = get(user);
        return !myVal.equals(paramVal);
    }
}
