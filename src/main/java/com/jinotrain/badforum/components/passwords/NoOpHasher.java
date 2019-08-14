package com.jinotrain.badforum.components.passwords;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0x7FFFFFFF)
public class NoOpHasher extends PasswordHasher
{
    @Override
    protected String getPrefix()
    {
        return "noop";
    }


    @Override
    protected String hash(String password)
    {
        throw new SecurityException("attempted to no-op encrypt password: this is never okay");
    }


    @Override
    protected boolean checkHash(String password, String hash)
    {
        return password.equals(hash);
    }
}
