package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.repositories.ForumSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;

public class ForumSessionInjector extends HandlerInterceptorAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(ForumSessionInjector.class);

    @Autowired
    private ForumSessionRepository sessionRepository;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception ignore) {}

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

        Optional<ForumSession> possibleSession = sessionRepository.findById(sessionCookie.getValue());

        if (!possibleSession.isPresent())
        {
            Cookie deleteCookie = (Cookie)sessionCookie.clone();
            deleteCookie.setMaxAge(0);
            deleteCookie.setValue(null);
            response.addCookie(deleteCookie);
            return true;
        }

        ForumSession session = possibleSession.get();

        if (session.getExpireTime().isBefore(Instant.now()))
        {
            sessionRepository.delete(session);
        }
        else
        {
            session.refreshExpireTime();
            request.setAttribute("forumSession", session);
        }

        return true;
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav)
    {
        if (mav == null) { return; }

        ForumSession session = (ForumSession)request.getAttribute("forumSession");
        if (session != null) { mav.addObject("forumSession", session); }
    }
}
