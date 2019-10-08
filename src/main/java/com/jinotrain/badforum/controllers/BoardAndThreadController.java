package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class BoardAndThreadController extends ForumController
{
    private static Logger logger = LoggerFactory.getLogger(BoardAndThreadController.class);


    private ModelAndView getThread(ForumThread thread)
    {
        ThreadViewData viewData = getThreadViewData(thread, em);
        ModelAndView ret = new ModelAndView("viewthread.html");
        ret.addObject("threadViewData", viewData);
        return ret;
    }


    private ModelAndView getBoard(ForumBoard board)
    {
        BoardViewData viewData = getBoardViewData(board, em);
        ModelAndView ret = new ModelAndView("viewboard.html");
        ret.addObject("boardViewData", viewData);
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/")
    public ModelAndView viewTopLevelBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumBoard rootBoard = boardRepository.findRootBoard();
        return getBoard(rootBoard);
    }


    @Transactional
    @RequestMapping(value = "/board/*")
    public ModelAndView viewRequestedBoard(HttpServletRequest request, HttpServletResponse response)
    {
        String requestUrl = request.getServletPath();
        ForumBoard viewBoard;

        try
        {
            long boardID = Long.valueOf(requestUrl.substring("/board/".length()));
            viewBoard = boardRepository.findByIndex(boardID);
        }
        catch (NumberFormatException e)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewboard_notfound.html");
        }

        if (viewBoard == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewboard_notfound.html");
        }

        return getBoard(viewBoard);
    }


    @Transactional
    @RequestMapping("/thread/*")
    public ModelAndView viewRequestedThread(HttpServletRequest request, HttpServletResponse response)
    {
        String requestUrl = request.getServletPath();
        ForumThread viewThread;

        try
        {
            long threadID = Long.valueOf(requestUrl.substring("/thread/".length()));
            viewThread = threadRepository.findByIndex(threadID);
        }
        catch (NumberFormatException e)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewthread_notfound.html");
        }

        if (viewThread == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("viewthread_notfound.html");
        }

        return getThread(viewThread);
    }



    @Transactional
    @RequestMapping(value = "/post", method = RequestMethod.POST)
    public ModelAndView postOrReply(HttpServletRequest request, HttpServletResponse response)
    {
        if (request.getParameter("boardID") != null)
        {
            return postNewTopic(request, response);
        }

        if (request.getParameter("threadID") != null)
        {
            return postReply(request, response);
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ModelAndView mav = new ModelAndView("post_error.html");
        mav.addObject("errorCode", "NO_DESTINATION");
        mav.addObject("postTopic", request.getParameter("topic"));
        mav.addObject("postText",  request.getParameter("text"));
        return mav;
    }


    private ModelAndView postNewTopic(HttpServletRequest request, HttpServletResponse response)
    {
        ForumBoard targetBoard;

        try
        {
            long boardID = Long.valueOf(request.getParameter("boardID"));
            targetBoard = boardRepository.findByIndex(boardID);
        }
        catch (NumberFormatException e)
        {
            targetBoard = null;
        }

        String postTopic = request.getParameter("topic");
        String postText  = request.getParameter("text");

        if (targetBoard == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ModelAndView mav = new ModelAndView("post_error.html");
            mav.addObject("errorCode", "BOARD_NOT_FOUND");
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        Boolean noTopic = postTopic == null || postTopic.isEmpty();
        Boolean noText  = postText  == null || postText.isEmpty();

        if (noTopic || noText)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ModelAndView mav = getBoard(targetBoard);

            if (noTopic && noText)
            {
                mav.addObject("postError", "Post is empty");
            }
            else if (noTopic)
            {
                mav.addObject("postError", "Post topic is missing");
                mav.addObject("postText", postText);
            }
            else
            {
                mav.addObject("postError", "Post text is missing");
                mav.addObject("postTopic", postTopic);
            }

            return mav;
        }

        ForumUser poster = getUserFromRequest(request);

        long threadIndex = threadRepository.getHighestIndex() + 1;

        ForumPost   firstPost = new ForumPost(postRepository.getHighestIndex() + 1, postText, poster);
        ForumThread newThread = new ForumThread(threadIndex, postTopic);

        newThread.setBoard(targetBoard);
        firstPost.setThread(newThread);

        threadRepository.saveAndFlush(newThread);
        postRepository.saveAndFlush(firstPost);

        return new ModelAndView("redirect:/thread/" + threadIndex);
    }


    private ModelAndView postReply(HttpServletRequest request, HttpServletResponse response)
    {
        Long threadID = null;
        ForumThread targetThread;

        try
        {
            threadID = Long.valueOf(request.getParameter("threadID"));
            targetThread = threadRepository.findById(threadID).orElse(null);
        }
        catch (NumberFormatException e)
        {
            targetThread = null;
        }

        String postText = request.getParameter("text");

        if (targetThread == null)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            ModelAndView mav = new ModelAndView("post_error.html");
            mav.addObject("errorCode", "THREAD_NOT_FOUND");
            mav.addObject("postTopic", request.getParameter(null));
            mav.addObject("postText",  request.getParameter(postText));
            return mav;
        }

        if (postText == null || postText.isEmpty())
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ModelAndView mav = getThread(targetThread);
            mav.addObject("postError", "Post is empty");
            return mav;
        }

        ForumUser poster = getUserFromRequest(request);

        ForumPost reply = new ForumPost(postRepository.getHighestIndex() + 1, postText, poster);

        reply.setThread(targetThread);
        postRepository.saveAndFlush(reply);

        return new ModelAndView("redirect:/thread/" + targetThread.getIndex());
    }
}