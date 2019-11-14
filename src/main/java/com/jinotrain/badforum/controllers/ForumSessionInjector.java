package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

public class ForumSessionInjector extends HandlerInterceptorAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(ForumSessionInjector.class);

    @Autowired
    private ForumSessionRepository sessionRepository;

    @Autowired
    FloodProtectionService floodProtectionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception ignore) {}

        boolean flooding = !floodProtectionService.updateIfNotFlooding(FloodCategory.ANY, request.getRemoteAddr());

        if (flooding)
        {
            request.setAttribute("flooding", true);
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) { return true; }

        Cookie sessionCookie = null;

        for (Cookie c: cookies)
        {
            if (c.getName().equals("forumSession"))
            {
                sessionCookie = c;
                break;
            }
        }

        if (sessionCookie == null) { return true; }

        ForumSession session = sessionRepository.findById(sessionCookie.getValue()).orElse(null);

        if (session == null)
        {
            Cookie deleteCookie = (Cookie)sessionCookie.clone();
            deleteCookie.setMaxAge(0);
            deleteCookie.setValue(null);
            response.addCookie(deleteCookie);
            return true;
        }

        if (session.getExpireTime().isBefore(Instant.now()))
        {
            sessionRepository.delete(session);
        }
        else
        {
            session.refreshExpireTime();
            sessionRepository.saveAndFlush(session);
            request.setAttribute("forumSession", session);
        }

        return true;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav)
    {
        if (mav == null) { return; }

        ForumSession session = (ForumSession)request.getAttribute("forumSession");

        if (session != null)
        {
            mav.addObject("forumSession", session);

            ForumUser user = session.getUser();
            if (user != null) { mav.addObject("user", user); }
        }
    }
}
