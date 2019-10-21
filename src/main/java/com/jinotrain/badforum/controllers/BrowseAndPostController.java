package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class BrowseAndPostController extends ForumController
{
    private static Logger logger = LoggerFactory.getLogger(BrowseAndPostController.class);


    private ModelAndView getBoard(ForumBoard board, ForumUser viewer)
    {
        BoardViewData viewData;

        try
        {
            viewData = getBoardViewData(board, viewer, em);
        }
        catch (SecurityException e)
        {
            return errorPage("viewboard_error.html", "NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }

        ModelAndView ret = new ModelAndView("viewboard.html");
        ret.addObject("boardViewData", viewData);
        ret.addObject("canMakeBoards", userHasPermission(viewer, UserPermission.MANAGE_BOARDS));
        ret.addObject("canPost", userHasBoardPermission(viewer, board, BoardPermission.POST));
        return ret;
    }


    private ModelAndView getThread(ForumThread thread, ForumUser viewer)
    {
        ThreadViewData viewData;

        try
        {
            viewData = getThreadViewData(thread, viewer, em);
        }
        catch (SecurityException e)
        {
            return errorPage("viewthread_error.html", "NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }

        ModelAndView ret = new ModelAndView("viewthread.html");
        ret.addObject("threadViewData", viewData);
        ret.addObject("canPost", userHasBoardPermission(viewer, thread.getBoard(), BoardPermission.POST));
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/")
    public ModelAndView viewTopLevelBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumBoard rootBoard = boardRepository.findRootBoard();
        return getBoard(rootBoard, getUserFromRequest(request));
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

        return getBoard(viewBoard, getUserFromRequest(request));
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

        return getThread(viewThread, getUserFromRequest(request));
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
            ModelAndView mav = errorPage("post_error.html", "BOARD_NOT_FOUND", HttpStatus.NOT_FOUND);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        ForumUser poster = getUserFromRequest(request);

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.FORBIDDEN);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getBoard(targetBoard, poster);
            mav.setStatus(HttpStatus.FORBIDDEN);
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
            ModelAndView mav = errorPage("post_error.html", "THREAD_NOT_FOUND", HttpStatus.NOT_FOUND);
            mav.addObject("postText",  postText);
            return mav;
        }

        ForumUser  poster      = getUserFromRequest(request);
        ForumBoard targetBoard = targetThread.getBoard();

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.FORBIDDEN);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getThread(targetThread, poster);
            mav.setStatus(HttpStatus.FORBIDDEN);
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

        return new ModelAndView("redirect:/thread/" + targetThread.getIndex());
    }


    @Transactional
    @RequestMapping(value = "/board/new", method = RequestMethod.POST)
    public ModelAndView createNewBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user = getUserFromRequest(request);

        if (!userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("newboard_error.html", "NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }

        String parentIndexRaw = request.getParameter("parentIndex");
        long parentIndex;

        if (parentIndexRaw == null || parentIndexRaw.isEmpty())
        {
            return errorPage("newboard_error.html", "MISSING_PARENT", HttpStatus.BAD_REQUEST);
        }

        try
        {
            parentIndex = Long.valueOf(parentIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("newboard_error.html", "PARENT_INDEX_INVALID", HttpStatus.BAD_REQUEST);
        }

        ForumBoard parentBoard = boardRepository.findByIndex(parentIndex);

        if (parentBoard == null)
        {
            return errorPage("newboard_error.html", "NO_PARENT", HttpStatus.NOT_FOUND);
        }

        String newBoardName = request.getParameter("boardName");

        if (newBoardName == null || newBoardName.isEmpty())
        {
            return errorPage("newboard_error.html", "NO_BOARD_NAME", HttpStatus.BAD_REQUEST);
        }

        ForumBoard newBoard = new ForumBoard(boardRepository.getHighestIndex()+1, newBoardName);
        newBoard.setParentBoard(parentBoard);
        boardRepository.saveAndFlush(newBoard);

        return new ModelAndView("redirect:/board/" + newBoard.getIndex());
    }
}