package com.jinotrain.badforum.components.passwords;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

@Component
@Order(1)
public class BCryptHasher extends PasswordHasher
{
    private final static int WORK_FACTOR = 8;

    @Override
    protected String getPrefix()
    {
        return "bcrypt";
    }


    @Override
    public String hash(String password)
    {
        return BCrypt.hashpw(password, BCrypt.gensalt(WORK_FACTOR));
    }

    //                      |--------salt--------|-------------hash-------------|
    // example hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
    //                |  |
    //               id  |
    //                work factor
    //

    @Override
    protected String checkHash(String password, String hash)
    {
        boolean matches = BCrypt.checkpw(password, hash);

        if (matches) // we know it's a valid hash since it matched
        {
            // [0] = zero length, [1] = ID, [2] = work factor, [3] = salt/hash
            String[] parts = hash.split("\\$");
            int workFactor = Integer.valueOf(parts[2]);
            if (workFactor < WORK_FACTOR) { return hash(password); }
            return hash;
        }

        return null;
    }
}
