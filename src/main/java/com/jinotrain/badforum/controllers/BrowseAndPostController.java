package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.util.UserBannedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@Controller
public class BrowseAndPostController extends ForumController
{
    private ModelAndView getBoard(ForumBoard board, ForumUser viewer)
    {
        BoardViewData viewData;

        try
        {
            viewData = getBoardViewData(board, viewer, em);
        }
        catch (SecurityException e)
        {
            return errorPage("viewboard_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ModelAndView ret = new ModelAndView("viewboard.html");
        ret.addObject("boardViewData", viewData);
        ret.addObject("canPost", ForumUser.userHasBoardPermission(viewer, board, BoardPermission.POST));
        return ret;
    }


    private ModelAndView getThread(ForumThread thread, ForumUser viewer)
    {
        ThreadViewData viewData;

        try
        {
            viewData = getThreadViewData(thread, viewer);
        }
        catch (SecurityException e)
        {
            return errorPage("viewthread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard board = thread.getBoard();

        boolean canPost = board == null ? ForumUser.userHasPermission(viewer, UserPermission.MANAGE_BOARDS)
                                        : ForumUser.userHasBoardPermission(viewer, board, BoardPermission.POST);

        boolean canModerate = board == null ? ForumUser.userHasPermission(viewer, UserPermission.MANAGE_BOARDS)
                                            : ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE);

        ModelAndView ret = new ModelAndView("viewthread.html");
        ret.addObject("threadViewData", viewData);
        ret.addObject("canPost", canPost);
        return ret;
    }


    private void addMovedThreadDetails(ModelAndView mav, ForumThread thread)
    {
        long lastMoveIndex = -1;

        while (thread != null && thread.wasMoved())
        {
            lastMoveIndex = thread.getMoveIndex();
            thread = threadRepository.findByIndex(lastMoveIndex);
        }

        if (thread == null || thread.isDeleted())
        {
            mav.addObject("errorCode", "THREAD_DELETED");
            mav.setStatus(HttpStatus.GONE);
        }
        else
        {
            mav.addObject("movedThreadIndex", lastMoveIndex);
            mav.addObject("movedThreadTopic", thread.getTopic());
        }
    }


    @Transactional
    @RequestMapping(value = "/")
    public ModelAndView viewTopLevelBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumBoard rootBoard = boardRepository.findRootBoard();

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        return getBoard(rootBoard, user);
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
            return errorPage("viewboard_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (viewBoard == null)
        {
            return errorPage("viewboard_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        return getBoard(viewBoard, user);
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
            return errorPage("viewthread_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (viewThread == null)
        {
            return errorPage("viewthread_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (viewThread.isDeleted())
        {
            return errorPage("viewthread_error.html", "THREAD_DELETED", HttpStatus.GONE);
        }

        if (viewThread.wasMoved())
        {
            ModelAndView mav = errorPage("viewthread_error.html", "THREAD_MOVED", HttpStatus.CONFLICT);
            addMovedThreadDetails(mav, viewThread);
            return mav;
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        return getThread(viewThread, user);
    }



    @Transactional
    @RequestMapping(value = "/post")
    public ModelAndView postOrReply(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("post_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        if (request.getParameter("boardIndex") != null)
        {
            return postNewTopic(request, response);
        }

        if (request.getParameter("threadIndex") != null)
        {
            return postReply(request, response);
        }

        ModelAndView mav  = errorPage("post_error.html", "NO_DESTINATION", HttpStatus.BAD_REQUEST);
        mav.addObject("postTopic", request.getParameter("topic"));
        mav.addObject("postText",  request.getParameter("text"));
        return mav;
    }


    private ModelAndView postNewTopic(HttpServletRequest request, HttpServletResponse response)
    {
        ForumBoard targetBoard;

        try
        {
            long boardIndex = Long.valueOf(request.getParameter("boardIndex"));
            targetBoard = boardRepository.findByIndex(boardIndex);
        }
        catch (NumberFormatException e)
        {
            targetBoard = null;
        }

        String postTopic = request.getParameter("topic");
        String postText  = request.getParameter("text");

        if (targetBoard == null)
        {
            ModelAndView mav = errorPage("post_error.html", "BOARD_NOT_FOUND", HttpStatus.NOT_FOUND);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        ForumUser poster;
        try { poster = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getBoard(targetBoard, poster);
            mav.setStatus(HttpStatus.UNAUTHORIZED);
            mav.addObject("postError", "You aren't allowed to post on this board");
            mav.addObject("postText", postText);
            return mav;
        }

        Boolean noTopic = postTopic == null || postTopic.isEmpty();
        Boolean noText  = postText  == null || postText.isEmpty();

        if (noTopic || noText)
        {
            ModelAndView mav = getBoard(targetBoard, poster);
            mav.setStatus(HttpStatus.BAD_REQUEST);

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

        long threadIndex = threadRepository.getHighestIndex() + 1;

        ForumPost   firstPost = new ForumPost(postRepository.getHighestIndex() + 1, postText, poster);
        ForumThread newThread = new ForumThread(threadIndex, postTopic, targetBoard, poster);
        firstPost.setThread(newThread);

        threadRepository.saveAndFlush(newThread);
        postRepository.saveAndFlush(firstPost);

        return new ModelAndView("redirect:/thread/" + threadIndex);
    }


    private ModelAndView postReply(HttpServletRequest request, HttpServletResponse response)
    {
        ForumThread targetThread;

        try
        {
            long threadIndex = Long.valueOf(request.getParameter("threadIndex"));
            targetThread = threadRepository.findByIndex(threadIndex);
        }
        catch (NumberFormatException e)
        {
            targetThread = null;
        }

        String postText = request.getParameter("text");

        if (targetThread == null)
        {
            ModelAndView mav = errorPage("post_error.html", "THREAD_NOT_FOUND", HttpStatus.NOT_FOUND);
            mav.addObject("postText", postText);
            return mav;
        }

        if (targetThread.isDeleted())
        {
            ModelAndView mav = errorPage("post_error.html", "THREAD_DELETED", HttpStatus.GONE);
            mav.addObject("postText", postText);
            return mav;
        }

        if (targetThread.wasMoved())
        {
            ModelAndView mav = errorPage("post_error.html", "THREAD_MOVED", HttpStatus.CONFLICT);
            mav.addObject("postText", postText);
            addMovedThreadDetails(mav, targetThread);
            return mav;
        }

        ForumUser poster;
        try { poster = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        ForumBoard targetBoard = targetThread.getBoard();

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postText", postText);
            return mav;
        }

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getThread(targetThread, poster);
            mav.setStatus(HttpStatus.UNAUTHORIZED);
            mav.addObject("postError", "You aren't allowed to post on this board");
            mav.addObject("postText", postText);
            return mav;
        }


        if (postText == null || postText.isEmpty())
        {
            ModelAndView mav = getThread(targetThread, poster);
            mav.setStatus(HttpStatus.BAD_REQUEST);
            mav.addObject("postError", "Post is empty");
            return mav;
        }

        ForumPost reply = new ForumPost(postRepository.getHighestIndex() + 1, postText, poster);

        reply.setThread(targetThread);
        postRepository.saveAndFlush(reply);

        targetThread.setLastUpdate(Instant.now());

        return new ModelAndView("redirect:/thread/" + targetThread.getIndex());
    }
}

