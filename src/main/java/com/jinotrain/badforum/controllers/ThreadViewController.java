package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.db.entities.ForumThread;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ThreadViewController extends ForumController
{
    @Transactional
    @RequestMapping("/thread/*")
    public ModelAndView getThread(HttpServletRequest request,
                                  HttpServletResponse response)
    {
        String requestUrl = request.getServletPath();
        long threadID;

        try
        {
            threadID = Long.valueOf(requestUrl.substring("/thread/".length()));
        }
        catch (NumberFormatException e)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewthread_notfound.html");
        }

        ForumThread viewThread = threadRepository.findById(threadID).orElse(null);

        if (viewThread == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewthread_notfound.html");
        }

        ThreadViewData threadViewData = getThreadViewData(viewThread, em);

        ModelAndView mav = new ModelAndView("viewthread.html");
        mav.addObject("threadViewData", threadViewData);
        return mav;
    }
}
