package com.jinotrain.badforum.components.passwords;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumPasswordService
{
    @Autowired
    protected List<PasswordHasher> passwordHashers;

    public String checkAndUpgradePassword(String password, String checkhash)
    {
        if (password == null || checkhash == null) { return null; }

        boolean notFirstPick = false;
        PasswordHasher firstHasher = null;

        for (PasswordHasher hasher: passwordHashers)
        {
            String newHash = hasher.checkHashAndUpgrade(password, checkhash);

            if (newHash != null)
            {
                if (notFirstPick) { return firstHasher.hashAndPrefix(password); }
                return newHash;
            }

            notFirstPick = true;
            firstHasher  = hasher;
        }

        return null;
    }

    public String hashPassword(String password)
    {
        PasswordHasher hasher = passwordHashers.get(0);
        return hasher.hashAndPrefix(password);
    }
}
