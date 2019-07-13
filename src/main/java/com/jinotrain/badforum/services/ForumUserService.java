package com.jinotrain.badforum.services;

import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ForumUserService implements UserDetailsService
{
    @Autowired
    private ForumUserRepository userRepository;

    private static PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        return null;
    }


    public PasswordEncoder passwordEncoder()
    {
        if (passwordEncoder == null)
        {
            passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        return passwordEncoder;
    }
}
