package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ForumSessionInjector extends HandlerInterceptorAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(ForumSessionInjector.class);
    private static final int MAX_SESSIONS_PER_USER = 16;

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
            deleteCookie(sessionCookie, response);
            return;
        }

        ForumSession session = possibleSession.get();
        List<String> prunedIDs = pruneSessions(session.getUser());

        if (!prunedIDs.contains(session.getId()))
        {
            session.refreshExpireTime();
            mav.addObject("forumSession", session);
        }
    }


    private void deleteCookie(Cookie cookie, HttpServletResponse response)
    {
        Cookie deleteCookie = (Cookie)cookie.clone();
        deleteCookie.setMaxAge(0);
        deleteCookie.setValue(null);
        response.addCookie(deleteCookie);
    }


    private List<String> pruneSessions(ForumUser user)
    {
        List<String> prunedIDs = new ArrayList<>();

        List<ForumSession> userSessions = sessionRepository.findAllByUserOrderByExpireTimeDesc(user);
        Instant now = Instant.now();

        int sessionCount = 0;

        for (ForumSession session: userSessions)
        {
            if (sessionCount >= MAX_SESSIONS_PER_USER || session.getExpireTime().isBefore(now))
            {
                sessionRepository.delete(session);
                prunedIDs.add(session.getId());
            }
            else
            {
                sessionCount += 1;
            }
        }

        return prunedIDs;
    }
}
