package com.jinotrain.badforum.components;

import com.jinotrain.badforum.data.PreAdminKey;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumRoleRepository;
import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.security.SecureRandom;
import java.util.*;

@Component
public class PreAdminInitializer implements ApplicationListener<ContextRefreshedEvent>
{
    private static Logger logger = LoggerFactory.getLogger(PreAdminInitializer.class);

    private static char[] ID_CHARS   = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()".toCharArray();

    @Autowired
    private ForumUserRepository userRepository;

    @Autowired
    private ForumRoleRepository roleRepository;

    @Autowired
    private PreAdminKey preAdminKey;

    @PersistenceContext
    private EntityManager em;


    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        List<ForumRole> adminRoles = findOrCreateAdminRoles();

        if (noUsersWithRoles(adminRoles))
        {
            StringBuilder newCode = new StringBuilder();
            SecureRandom  rng     = new SecureRandom();

            for (int i = 0; i < 32; i++)
            {
                char next = ID_CHARS[rng.nextInt(ID_CHARS.length)];
                newCode.append(next);
            }

            String key = newCode.toString();
            preAdminKey.setKey(key);
            preAdminKey.setAdminRoleID(adminRoles.get(0).getId());

            logger.warn("----------------------------------------");
            logger.warn("");
            logger.warn("You currently have no administrators.");
            logger.warn("   To fix this, create a new user");
            logger.warn("    with the following password:");
            logger.warn("");
            logger.warn("  " + key);
            logger.warn("");
            logger.warn("----------------------------------------");
        }
    }


    private List<ForumRole> findOrCreateAdminRoles()
    {
        List<ForumRole> adminRoles = new ArrayList<>();

        for (ForumRole role: roleRepository.findAll())
        {
           if (role.isAdmin())
           {
               adminRoles.add(role);
               break;
           }
        }

        if (adminRoles.isEmpty())
        {
            ForumRole newAdminRole = new ForumRole("Global administrator", 0x7FFFFFFF);
            newAdminRole.setAdmin(true);
            roleRepository.save(newAdminRole);
            adminRoles.add(newAdminRole);
        }

        return adminRoles;
    }


    private boolean noUsersWithRoles(List<ForumRole> roles)
    {
        Set<Long> roleIDs = new HashSet<>();

        for (ForumRole role: roles)
        {
            roleIDs.add(role.getId());
        }

        for (ForumUser user: userRepository.findAll())
        {
            for (ForumRole userRole: user.getRoles())
            {
                if (roleIDs.contains(userRole.getId()))
                {
                    return false;
                }
            }
        }

        return true;
    }
}
