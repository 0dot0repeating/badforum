package com.jinotrain.badforum.components;

import com.jinotrain.badforum.components.hashers.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumPasswordService
{
    @Autowired
    protected List<PasswordHasher> passwordHashers;

    private static final Logger logger = LoggerFactory.getLogger(ForumPasswordService.class);

    public boolean passwordMatches(String password, String checkhash)
    {
        for (PasswordHasher hasher: passwordHashers)
        {
            logger.debug("Trying password/hash \"{}\"/\"{}\" on hasher {}", password, checkhash, hasher.getClass().getSimpleName());
            if (hasher.hashMatches(password, checkhash)) { return true; }
        }

        return false;
    }

    public String hashPassword(String password)
    {
        PasswordHasher hasher = passwordHashers.get(0);
        return hasher.hashAndPrefix(password);
    }
}
