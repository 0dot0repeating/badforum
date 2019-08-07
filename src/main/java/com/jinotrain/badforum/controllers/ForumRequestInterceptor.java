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
import java.util.Optional;

public class ForumRequestInterceptor extends HandlerInterceptorAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(ForumRequestInterceptor.class);

    @Autowired
    private ForumSessionRepository sessionRepository;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav)
    {
        // request handled by @ResponseBody, do nothing
        if (mav == null) { return; }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) { return; }

        Cookie sessionCookie = null;

        for (Cookie c: cookies)
        {
            if (c.getName().equals("forumSession"))
            {
                sessionCookie = c;
                break;
            }
        }

        if (sessionCookie == null) { return; }

        Optional<ForumSession> possibleSession = sessionRepository.findById(sessionCookie.getValue());

        if (!possibleSession.isPresent())
        {
            Cookie deleteCookie = (Cookie)sessionCookie.clone();
            deleteCookie.setMaxAge(0);
            deleteCookie.setValue(null);
            response.addCookie(deleteCookie);
            return;
        }

        mav.addObject("forumSession", possibleSession.get());
    }
}
