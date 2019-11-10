package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.*;
import com.jinotrain.badforum.util.MiscFuncs;
import com.jinotrain.badforum.util.UserBannedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BrowseAndPostController extends ForumController
{
    private int[] defaultRange(ForumUser user)
    {
        ForumPreferences prefs = user == null ? null : user.getPreferences();
        return new int[]{0, ForumPreferences.getPageSize(prefs)};
    }


    private int[] parseRange(String range)
    {
        String[] rangeParts = range.split("-");

        if (rangeParts.length == 2)
        {
            try
            {
                int start = Math.max(0, Integer.valueOf(rangeParts[0]) - 1);
                int end   = Math.max(1, Integer.valueOf(rangeParts[1]));

                if (end <= start) { return new int[]{start, start+1}; }
                return new int[]{start, end};
            }
            catch (NumberFormatException ignore)
            {
                return null;
            }
        }

        return null;
    }


    private int[] rangeFromRequest(HttpServletRequest request, ForumUser user)
    {
        String rangeStartRaw = request.getParameter("rangeStart");
        String rangeEndRaw   = request.getParameter("rangeEnd");

        if (rangeStartRaw != null && rangeEndRaw != null)
        {
            try
            {
                int start = Integer.valueOf(rangeStartRaw) - 1;
                int end   = Integer.valueOf(rangeEndRaw);

                if (end <= start) { return new int[]{start, start+1}; }
                return new int[]{start, end};
            }
            catch (NumberFormatException ignore)
            {
                return defaultRange(user);
            }
        }

        return defaultRange(user);
    }


    private List<String[]> createViewRange(int[] pageRange, long totalCount, String prefix)
    {
        int rangeStart = pageRange[0];
        int rangeEnd   = pageRange[1];
        int rangeSize  = rangeEnd - rangeStart;



        List<String[]> ret = new ArrayList<>();

        for (long i = 0; i < Math.max(rangeSize, totalCount); i += rangeSize)
        {
            long viewRangeStart = i + 1;
            long viewRangeEnd   = i + rangeSize;
            long pageIndex      = (i / rangeSize) + 1;

            boolean currentPage = MiscFuncs.clamp(viewRangeEnd, rangeStart + 1, rangeEnd) == viewRangeEnd;
            String  path        = prefix + (prefix.endsWith("/") ? "" : "/") + viewRangeStart + "-" + viewRangeEnd;
            String  linkText    = String.format(currentPage ? "[%s]" : "%s", pageIndex);

            ret.add(new String[]{path, linkText});
        }

        return ret;
    }


    private ModelAndView getBoard(ForumBoard board, ForumUser viewer, int[] threadRange)
    {
        BoardViewData viewData;

        try
        {
            viewData = getBoardViewData(board, viewer, threadRange, em);
        }
        catch (SecurityException e)
        {
            return errorPage("viewboard_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String prefix = "/board/" + viewData.index + "/";
        ModelAndView ret = new ModelAndView("viewboard.html");
        ret.addObject("boardViewData", viewData);
        ret.addObject("canPost", ForumUser.userHasBoardPermission(viewer, board, BoardPermission.POST));
        ret.addObject("viewRange", new int[]{threadRange[0] + 1, threadRange[1]});
        ret.addObject("pageLinks", createViewRange(threadRange, viewData.threadCount, prefix));
        return ret;
    }


    private ModelAndView getThread(ForumThread thread, ForumUser viewer, int[] postRange)
    {
        ThreadViewData viewData;

        try
        {
            viewData = getThreadViewData(thread, viewer, postRange, em);
        }
        catch (SecurityException e)
        {
            return errorPage("viewthread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard board = thread.getBoard();

        boolean canPost = board == null ? ForumUser.userHasPermission(viewer, UserPermission.MANAGE_BOARDS)
                                        : ForumUser.userHasBoardPermission(viewer, board, BoardPermission.POST);

        String prefix = "/thread/" + viewData.index + "/";
        ModelAndView ret = new ModelAndView("viewthread.html");
        ret.addObject("threadViewData", viewData);
        ret.addObject("canPost", canPost);
        ret.addObject("viewRange", new int[]{postRange[0] + 1, postRange[1]});
        ret.addObject("pageLinks", createViewRange(postRange, viewData.postCount, prefix));
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/")
    public ModelAndView viewTopLevelBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        ForumBoard rootBoard = boardRepository.findRootBoard();

        return getBoard(rootBoard, user, defaultRange(user));
    }


    @Transactional
    @RequestMapping(value = {"/board/*", "/board/*/*-*"})
    public ModelAndView viewRequestedBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        String[] urlParts = request.getServletPath().split("/");
        ForumBoard viewBoard;

        try
        {
            long boardID = Long.valueOf(urlParts[2]);
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

        int[] threadRange = defaultRange(user);

        if (urlParts.length == 4)
        {
            int[] newRange = parseRange(urlParts[3]);
            threadRange = newRange == null ? threadRange : newRange;
        }

        return getBoard(viewBoard, user, threadRange);
    }


    @Transactional
    @RequestMapping(value = {"/thread/*", "/thread/*/*-*"})
    public ModelAndView viewRequestedThread(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        String[] urlParts = request.getServletPath().split("/");
        ForumThread viewThread;

        try
        {
            long threadID = Long.valueOf(urlParts[2]);
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

        if (viewThread.wasMoved())
        {
            long moveIndex = -1;

            while (viewThread != null && viewThread.wasMoved())
            {
                moveIndex = viewThread.getMoveIndex();
                viewThread = threadRepository.findByIndex(moveIndex);
            }

            if (viewThread == null || viewThread.isDeleted())
            {
                return errorPage("viewthread_error.html", "THREAD_DELETED", HttpStatus.GONE);
            }

            return new ModelAndView("redirect:/thread/" + moveIndex);
        }

        if (viewThread.isDeleted())
        {
            return errorPage("viewthread_error.html", "THREAD_DELETED", HttpStatus.GONE);
        }

        int[] threadRange = defaultRange(user);

        if (urlParts.length == 4)
        {
            int[] newRange = parseRange(urlParts[3]);
            threadRange = newRange == null ? threadRange : newRange;
        }

        return getThread(viewThread, user, threadRange);
    }


    @Transactional
    @RequestMapping(value = "/board/*/post")
    public ModelAndView postNewTopic(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser poster;
        try { poster = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!request.getMethod().equals("POST"))
        {
            return errorPage("post_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        String[] urlParts = request.getServletPath().split("/");
        ForumBoard targetBoard;

        try
        {
            long boardIndex = Long.valueOf(urlParts[2]);
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

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getBoard(targetBoard, poster, rangeFromRequest(request, poster));
            mav.setStatus(HttpStatus.UNAUTHORIZED);
            mav.addObject("postError", "You aren't allowed to post on this board");
            mav.addObject("postText", postText);
            return mav;
        }

        Boolean noTopic = postTopic == null || postTopic.isEmpty();
        Boolean noText  = postText  == null || postText.isEmpty();

        if (noTopic || noText)
        {
            ModelAndView mav = getBoard(targetBoard, poster, rangeFromRequest(request, poster));
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


    @Transactional
    @RequestMapping(value = "/thread/*/post")
    public ModelAndView postReply(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser poster;
        try { poster = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!request.getMethod().equals("POST"))
        {
            return errorPage("post_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        String[] urlParts = request.getServletPath().split("/");
        ForumThread targetThread;

        try
        {
            long threadIndex = Long.valueOf(urlParts[2]);
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

        if (targetThread.wasMoved())
        {
            while (targetThread != null && targetThread.wasMoved())
            {
                targetThread = threadRepository.findByIndex(targetThread.getMoveIndex());
            }
        }

        if (targetThread == null || targetThread.isDeleted())
        {
            ModelAndView mav = errorPage("post_error.html", "THREAD_DELETED", HttpStatus.GONE);
            mav.addObject("postText", postText);
            return mav;
        }

        ForumBoard targetBoard = targetThread.getBoard();

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postText", postText);
            return mav;
        }

        if (!ForumUser.userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
        {
            ModelAndView mav = getThread(targetThread, poster, rangeFromRequest(request, poster));
            mav.setStatus(HttpStatus.UNAUTHORIZED);
            mav.addObject("postError", "You aren't allowed to post on this board");
            mav.addObject("postText", postText);
            return mav;
        }


        if (postText == null || postText.isEmpty())
        {
            ModelAndView mav = getThread(targetThread, poster, rangeFromRequest(request, poster));
            mav.setStatus(HttpStatus.BAD_REQUEST);
            mav.addObject("postError", "Post is empty");
            return mav;
        }

        ForumPost reply = new ForumPost(postRepository.getHighestIndex() + 1, postText, poster);

        long newPostCount = em.createNamedQuery("ForumThread.getPostCount", Long.class)
                              .setParameter("threadID", targetThread.getID())
                              .getSingleResult() + 1;

        int[] viewRange = rangeFromRequest(request, poster);
        int   rangeSize = viewRange[1] - viewRange[0];

        long newRangeEnd   = ((newPostCount + rangeSize - 1) / rangeSize) * rangeSize;
        long newRangeStart = newRangeEnd - rangeSize + 1;


        reply.setThread(targetThread);
        postRepository.saveAndFlush(reply);

        targetThread.setLastUpdate(Instant.now());

        return new ModelAndView("redirect:/thread/" + targetThread.getIndex() + "/" + newRangeStart + "-" + newRangeEnd);
    }
}

