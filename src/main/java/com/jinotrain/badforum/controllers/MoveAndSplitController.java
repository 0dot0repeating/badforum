package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.data.BoardViewData;
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
import java.util.*;

@Controller
public class MoveAndSplitController extends ForumController
{
    private BoardViewData getBoardTree(ForumUser viewer)
    {
        return getBoardTree(viewer, boardRepository.findRootBoard());
    }

    private BoardViewData getBoardTree(ForumUser viewer, ForumBoard root) throws SecurityException
    {
        if (!ForumUser.userHasBoardPermission(viewer, root, BoardPermission.VIEW))
        {
            throw new SecurityException("user isn't allowed to view this board");
        }

        Map<Long, BoardViewData> currentLayer = new HashMap<>();

        // built up while adding boards to the tree data in the current layer
        Map<Long, BoardViewData> nextLayer;

        BoardViewData rootData = new BoardViewData(root.getIndex(), root.getName());
        rootData.canModerate = ForumUser.userHasBoardPermission(viewer, root, BoardPermission.MODERATE);
        rootData.childBoards = new ArrayList<>();
        currentLayer.put(rootData.index, rootData);

        while (!currentLayer.isEmpty())
        {
            nextLayer = new HashMap<>();

            for (ForumBoard b: boardRepository.findAllByParentIndexIn(currentLayer.keySet()))
            {
                if (!ForumUser.userHasBoardPermission(viewer, b, BoardPermission.VIEW)) { continue; }

                long index = b.getIndex();
                BoardViewData newBoardData = new BoardViewData(index, b.getName());
                newBoardData.canModerate = ForumUser.userHasBoardPermission(viewer, b, BoardPermission.MODERATE);
                newBoardData.childBoards = new ArrayList<>();

                long parentIndex = b.getParentBoard().getIndex();
                currentLayer.get(parentIndex).childBoards.add(newBoardData);
                nextLayer.put(index, newBoardData);
            }

            currentLayer = nextLayer;
        }

        return rootData;
    }


    @Transactional
    @RequestMapping(value = "/thread/*/move")
    public ModelAndView beginMovingThread(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        ForumUser viewer;
        try { viewer = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        long threadIndex;

        try
        {
            String path = request.getServletPath();
            String rawIndex = path.substring("/thread/".length(), path.length() - "/move".length());
            threadIndex = Long.valueOf(rawIndex);
        }
        catch (NumberFormatException e)
        {
            return errorPage("movethread_error.html", "INVALID_THREAD_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumThread thread = threadRepository.findByIndex(threadIndex);
        if (thread == null)    { return errorPage("movethread_error.html", "THREAD_NOT_FOUND", HttpStatus.NOT_FOUND); }
        if (thread.wasMoved()) { return errorPage("movethread_error.html", "ALREADY_MOVED",    HttpStatus.CONFLICT); }

        if (!ForumUser.userOutranksOrIs(viewer, thread.getAuthor()))
        {
            return errorPage("movethread_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard board = thread.getBoard();

        if (board == null ? !ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                          : !ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE))
        {
            return errorPage("movethread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ModelAndView test = new ModelAndView("movethread.html");
        test.addObject("threadTopic", thread.getTopic());
        test.addObject("threadIndex", threadIndex);
        test.addObject("boardViewData", getBoardTree(viewer));
        test.addObject("startingIndex", (board == null ? boardRepository.findRootBoard() : board).getIndex());

        return test;
    }


    @Transactional
    @RequestMapping(value = "/post/*/split")
    public ModelAndView beginSplittingPost(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        ForumUser viewer;
        try { viewer = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        long postIndex;

        try
        {
            String path = request.getServletPath();
            String rawIndex = path.substring("/post/".length(), path.length() - "/split".length());
            postIndex = Long.valueOf(rawIndex);
        }
        catch (NumberFormatException e)
        {
            return errorPage("splitpost_error.html", "INVALID_POST_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumPost post = postRepository.findByIndex(postIndex);
        if (post == null)     { return errorPage("splitpost_error.html", "POST_NOT_FOUND", HttpStatus.NOT_FOUND); }
        if (post.isDeleted()) { return errorPage("splitpost_error.html", "DELETED",        HttpStatus.CONFLICT); }
        if (post.wasSplit())  { return errorPage("splitpost_error.html", "ALREADY_SPLIT",  HttpStatus.CONFLICT); }

        ForumThread thread = post.getThread();
        ForumBoard board   = thread == null ? null : thread.getBoard();

        if (board == null ? !ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                          : !ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE))
        {
            return errorPage("splitpost_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        if (!ForumUser.userOutranksOrIs(viewer, post.getAuthor()))
        {
            return errorPage("splitpost_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        ModelAndView test = new ModelAndView("splitpost.html");
        test.addObject("threadTopic", thread == null ? null : thread.getTopic());
        test.addObject("threadIndex", thread == null ? null : thread.getIndex());
        test.addObject("postIndex", postIndex);
        test.addObject("postText",  formatPostText(post.getPostText()));
        test.addObject("boardViewData", getBoardTree(viewer));
        test.addObject("startingIndex", (board == null ? boardRepository.findRootBoard() : board).getIndex());

        return test;
    }


    @Transactional
    @RequestMapping(value = "/movethread")
    public ModelAndView moveThread(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        if (!request.getMethod().equals("POST"))
        {
            return errorPage("movethread_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser viewer;
        try { viewer = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        long threadIndex, boardIndex;

        try
        {
            threadIndex = Long.valueOf(request.getParameter("threadIndex"));
        }
        catch (NumberFormatException e)
        {
            return errorPage("movethread_error.html", "INVALID_THREAD_INDEX", HttpStatus.BAD_REQUEST);
        }

        try
        {
            boardIndex = Long.valueOf(request.getParameter("boardIndex"));
        }
        catch (NumberFormatException e)
        {
            return errorPage("movethread_error.html", "INVALID_BOARD_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumThread threadToMove = threadRepository.findByIndex(threadIndex);

        if (threadToMove == null)
        {
            return errorPage("movethread_error.html", "THREAD_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (threadToMove.isDeleted())
        {
            return errorPage("movethread_error.html", "DELETED", HttpStatus.GONE);
        }

        if (threadToMove.wasMoved())
        {
            return errorPage("movethread_error.html", "ALREADY_MOVED", HttpStatus.CONFLICT);
        }

        ForumBoard threadBoard = threadToMove.getBoard();

        if (threadBoard == null ? !ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                                : !ForumUser.userHasBoardPermission(viewer, threadBoard, BoardPermission.MODERATE))
        {
            return errorPage("movethread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        if (!ForumUser.userOutranksOrIs(viewer, threadToMove.getAuthor()))
        {
            return errorPage("movethread_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard targetBoard = boardRepository.findByIndex(boardIndex);

        if (targetBoard == null)
        {
            return errorPage("movethread_error.html", "BOARD_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (threadBoard != null && boardIndex == threadBoard.getIndex())
        {
            return errorPage("movethread_error.html", "ALREADY_THERE", HttpStatus.BAD_REQUEST);
        }

        if (!ForumUser.userHasBoardPermission(viewer, targetBoard, BoardPermission.MODERATE))
        {
            return errorPage("movethread_error.html", "NOT_ALLOWED_TARGET", HttpStatus.UNAUTHORIZED);
        }

        long newIndex = threadRepository.getHighestIndex() + 1;
        ForumThread newThread = ForumThread.transferPostsToNewThread(newIndex, threadToMove, targetBoard);

        threadToMove.setThreadMoved(newIndex);
        threadRepository.save(newThread);

        ModelAndView ret = new ModelAndView("movethread_success.html");
        ret.addObject("newThreadIndex", newIndex);
        ret.addObject("threadTopic", newThread.getTopic());
        ret.addObject("newBoardIndex", boardIndex);
        ret.addObject("newBoardName", targetBoard.getName());

        if (threadBoard != null)
        {
            ret.addObject("oldBoardIndex", threadBoard.getIndex());
            ret.addObject("oldBoardName", threadBoard.getName());
        }

        return ret;
    }


    @Transactional
    @RequestMapping(value = "/splitpost")
    public ModelAndView splitPost(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        if (!request.getMethod().equals("POST"))
        {
            return errorPage("splitpost_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser viewer;
        try { viewer = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        long postIndex, boardIndex;

        try
        {
            postIndex = Long.valueOf(request.getParameter("postIndex"));
        }
        catch (NumberFormatException e)
        {
            return errorPage("splitpost_error.html", "INVALID_POST_INDEX", HttpStatus.BAD_REQUEST);
        }

        try
        {
            boardIndex = Long.valueOf(request.getParameter("boardIndex"));
        }
        catch (NumberFormatException e)
        {
            return errorPage("splitpost_error.html", "INVALID_BOARD_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumPost postToSplit = postRepository.findByIndex(postIndex);

        if (postToSplit == null)
        {
            return errorPage("splitpost_error.html", "POST_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (postToSplit.isDeleted())
        {
            return errorPage("splitpost_error.html", "DELETED", HttpStatus.GONE);
        }

        if (postToSplit.wasSplit())
        {
            return errorPage("splitpost_error.html", "ALREADY_SPLIT", HttpStatus.CONFLICT);
        }

        ForumThread postThread = postToSplit.getThread();
        ForumBoard  postBoard  = postThread == null ? null : postThread.getBoard();

        if (postBoard == null ? !ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                              : !ForumUser.userHasBoardPermission(viewer, postBoard, BoardPermission.MODERATE))
        {
            return errorPage("splitpost_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        if (!ForumUser.userOutranksOrIs(viewer, postToSplit.getAuthor()))
        {
            return errorPage("splitpost_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard targetBoard = boardRepository.findByIndex(boardIndex);

        if (targetBoard == null)
        {
            return errorPage("splitpost_error.html", "BOARD_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (postBoard != null && postThread.getPosts().size() == 1 && postBoard.getIndex() == boardIndex)
        {
            return errorPage("splitpost_error.html", "ALREADY_THERE", HttpStatus.BAD_REQUEST);
        }

        if (!ForumUser.userHasBoardPermission(viewer, targetBoard, BoardPermission.MODERATE))
        {
            return errorPage("splitpost_error.html", "NOT_ALLOWED_TARGET", HttpStatus.UNAUTHORIZED);
        }

        long newThreadIndex = threadRepository.getHighestIndex() + 1;
        long newPostIndex   = postRepository.getHighestIndex() + 1;

        String postTopic = request.getParameter("threadTopic");

        if (postTopic == null || postTopic.isEmpty())
        {
            postTopic = postThread == null ? "Split post" : "[Split] " + postThread.getTopic();
        }

        ForumThread newThread = new ForumThread(newThreadIndex, postTopic, targetBoard, postToSplit.getAuthor());
        ForumPost   newPost   = new ForumPost(newPostIndex, postToSplit, newThread);

        postToSplit.setPostSplit(newPostIndex, newThreadIndex);
        threadRepository.save(newThread);
        postRepository.save(newPost);

        ModelAndView ret = new ModelAndView("splitpost_success.html");
        ret.addObject("newThreadIndex", newThreadIndex);
        ret.addObject("newThreadTopic", postTopic);

        if (postBoard == null || postBoard.getIndex() != boardIndex)
        {
            ret.addObject("newBoardIndex", boardIndex);
            ret.addObject("newBoardName", targetBoard.getName());

            if (postBoard != null)
            {
                ret.addObject("oldBoardIndex", postBoard.getIndex());
                ret.addObject("oldBoardName",  postBoard.getName());
            }
        }
        else
        {
            ret.addObject("oldBoardIndex", boardIndex);
            ret.addObject("oldBoardName", targetBoard.getName());
        }

        return ret;
    }
}
