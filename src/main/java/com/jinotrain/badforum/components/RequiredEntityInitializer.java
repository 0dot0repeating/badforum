package com.jinotrain.badforum.components;

import com.jinotrain.badforum.data.PreAdminKey;
import com.jinotrain.badforum.db.entities.*;
import com.jinotrain.badforum.db.repositories.*;
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
public class RequiredEntityInitializer implements ApplicationListener<ContextRefreshedEvent>
{
    private static Logger logger = LoggerFactory.getLogger(RequiredEntityInitializer.class);

    private static char[] ID_CHARS   = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()".toCharArray();

    @Autowired
    private ForumUserRepository userRepository;

    @Autowired
    private ForumRoleRepository roleRepository;

    @Autowired
    private ForumBoardRepository boardRepository;

    @Autowired
    private ForumThreadRepository threadRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private PreAdminKey preAdminKey;

    @PersistenceContext
    private EntityManager em;


    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        initAdminStuff();
        initRootBoard();
        initDefaultRole();
    }


    private void initAdminStuff()
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


    private void initRootBoard()
    {
        ForumBoard currentRoot = boardRepository.findRootBoard();

        if (currentRoot == null)
        {
            logger.info("No root board found, creating one along with test thread");

            currentRoot = new ForumBoard("Root board");
            currentRoot.setRootBoard(true);
            boardRepository.save(currentRoot);

            ForumThread testThread = new ForumThread("Test thread");
            ForumPost testPost = new ForumPost("Test post, please ignore (or delete if you're the admin)");

            testThread.setBoard(currentRoot);
            threadRepository.save(testThread);

            testPost.setThread(testThread);
            postRepository.save(testPost);
        }
    }


    private void initDefaultRole()
    {
        ForumRole currentDefault = roleRepository.findDefaultRole();

        if (currentDefault == null)
        {
            logger.info("No default user role found, creating one");

            currentDefault = new ForumRole("All users", 0);
            currentDefault.setDefaultRole(true);
            roleRepository.saveAndFlush(currentDefault);

            // the role system needs every user to have this role, or else it'll treat them as anonymous guests
            for (ForumUser user: userRepository.findAll())
            {
                user.addRole(currentDefault);
            }
        }
    }
}
