package com.jinotrain.badforum.components.passwords;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.mindrot.jbcrypt.BCrypt;

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


    @Override
    protected boolean checkHash(String password, String hash)
    {
        return BCrypt.checkpw(password, hash);
    }
}
