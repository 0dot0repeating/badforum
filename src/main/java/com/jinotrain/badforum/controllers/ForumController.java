package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumRoleRepository;
import com.jinotrain.badforum.db.repositories.ForumSessionRepository;
import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

class ForumController
{
    @Autowired
    FloodProtectionService floodProtectionService;

    @Autowired
    ForumSessionRepository sessionRepository;

    @Autowired
    ForumUserRepository userRepository;

    @Autowired
    ForumRoleRepository roleRepository;


    ForumSession getForumSession(HttpServletRequest request)
    {
        return getForumSession(request, false);
    }

    ForumSession getForumSession(HttpServletRequest request, boolean refresh)
    {
        Object possibleSession = request.getAttribute("forumSession");

        if (possibleSession instanceof ForumSession)
        {
            ForumSession session = (ForumSession)possibleSession;
            if (refresh) { session = sessionRepository.findById(session.getId()).orElse(null); }
            return session;
        }

        return null;
    }


    ForumUser getUserFromRequest(HttpServletRequest request)
    {
        ForumSession session = getForumSession(request);
        if (session == null) { return null; }

        ForumUser user = session.getUser();
        if (user != null) { user = userRepository.findByUsernameIgnoreCase(user.getUsername()); }
        return user;
    }
}
