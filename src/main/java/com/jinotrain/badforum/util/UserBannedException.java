package com.jinotrain.badforum.util;

import java.time.Instant;

public class UserBannedException extends Exception
{
    private static final long serialVersionUID = 1;

    private String  banReason;
    private Instant bannedUntil;
    private String  sessionID;

    public UserBannedException(String banReason, Instant bannedUntil, String sessionID)
    {
        this.bannedUntil = bannedUntil;
        this.banReason   = banReason;
        this.sessionID   = sessionID;
    }

    public Instant getBannedUntil() { return bannedUntil; }
    public String  getBanReason()   { return banReason; }
    public String  getSessionID()   { return sessionID; }

    @Override
    public String getMessage() { return banReason; }

    @Override
    public String getLocalizedMessage() { return banReason; }
}
