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
import java.time.Instant;

@Controller
public class BoardThreadPostController extends ForumController
{
    private static Logger logger = LoggerFactory.getLogger(BoardThreadPostController.class);


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
        ret.addObject("canManageBoard", userHasPermission(viewer, UserPermission.MANAGE_BOARDS));
        ret.addObject("canPost", userHasBoardPermission(viewer, board, BoardPermission.POST));
        ret.addObject("canModerate", userHasBoardPermission(viewer, board, BoardPermission.MODERATE));
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
            return errorPage("viewthread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard board = thread.getBoard();

        ModelAndView ret = new ModelAndView("viewthread.html");
        ret.addObject("threadViewData", viewData);
        ret.addObject("canPost", userHasBoardPermission(viewer, board, BoardPermission.POST));
        ret.addObject("canModerate", userHasBoardPermission(viewer, board, BoardPermission.MODERATE));
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

        ForumUser poster = getUserFromRequest(request);

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postTopic", postTopic);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
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
        ForumThread newThread = new ForumThread(threadIndex, postTopic);

        newThread.setBoard(targetBoard);
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
            mav.addObject("postText",  postText);
            return mav;
        }

        ForumUser  poster      = getUserFromRequest(request);
        ForumBoard targetBoard = targetThread.getBoard();

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.VIEW))
        {
            ModelAndView mav = errorPage("post_error.html", "NOT_ALLOWED_TO_VIEW", HttpStatus.UNAUTHORIZED);
            mav.addObject("postText",  postText);
            return mav;
        }

        if (!userHasBoardPermission(poster, targetBoard, BoardPermission.POST))
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

        return new ModelAndView("redirect:/thread/" + targetThread.getIndex());
    }


    @Transactional
    @RequestMapping(value = "/board/new", method = RequestMethod.POST)
    public ModelAndView createNewBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user = getUserFromRequest(request);

        if (!userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("newboard_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
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


    @Transactional
    @RequestMapping(value = "/board/delete", method = RequestMethod.POST)
    public ModelAndView deleteBoard(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user = getUserFromRequest(request);

        if (!userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("deleteboard_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String boardIndexRaw = request.getParameter("boardIndex");

        if (boardIndexRaw == null || boardIndexRaw.isEmpty())
        {
            return errorPage("deleteboard_error.html", "MISSING_INDEX", HttpStatus.BAD_REQUEST);
        }

        long boardIndex;

        try
        {
            boardIndex = Long.valueOf(boardIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("deleteboard_error.html", "INVALID_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumBoard board = boardRepository.findByIndex(boardIndex);

        if (board == null)
        {
            return errorPage("deleteboard_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (board.isRootBoard())
        {
            return errorPage("deleteboard_error.html", "CANT_DELETE_ROOT", HttpStatus.FORBIDDEN);
        }

        ForumBoard parentBoard = board.getParentBoard();
        String boardName = board.getName();

        em.createQuery("DELETE FROM RoleToBoardLink l WHERE l.board.id = :boardID")
          .setParameter("boardID", board.getId())
          .executeUpdate();

        boardRepository.delete(board);

        ModelAndView ret = new ModelAndView("deleteboard.html");
        ret.addObject("boardName", boardName);
        ret.addObject("parentIndex", parentBoard == null ? -1 : parentBoard.getIndex());
        ret.addObject("parentName", parentBoard == null ? null : parentBoard.getName());
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/thread/delete", method = RequestMethod.POST)
    public ModelAndView deleteThread(HttpServletRequest request, HttpServletResponse response)
    {
        String threadIndexRaw = request.getParameter("threadIndex");

        if (threadIndexRaw == null)
        {
            return errorPage("deletethread_error.html", "MISSING_INDEX", HttpStatus.BAD_REQUEST);
        }

        long threadIndex;

        try
        {
            threadIndex = Long.valueOf(threadIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("deletethread_error.html", "INVALID_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumThread thread = threadRepository.findByIndex(threadIndex);

        if (thread == null)
        {
            return errorPage("deletethread_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        String threadTopic = thread.getTopic();

        ForumBoard board = thread.getBoard();
        ForumUser user = getUserFromRequest(request);

        if (board == null ? !userHasPermission(user, UserPermission.MANAGE_DETACHED)
                          : !userHasBoardPermission(user, board, BoardPermission.MODERATE))
        {
            return errorPage("deletethread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String keepPostsRaw = request.getParameter("keepPosts");
        boolean deletePosts = !("true".equalsIgnoreCase(keepPostsRaw) || "1".equals(keepPostsRaw));

        for (ForumPost post: thread.getPosts())
        {
            if (deletePosts) { post.deleteContents(); }
            post.setThread(null);
        }

        thread.setBoard(null);
        threadRepository.delete(thread);

        ModelAndView ret = new ModelAndView("deletethread.html");
        ret.addObject("boardIndex", board == null ? -1   : board.getIndex());
        ret.addObject("boardName",  board == null ? null : board.getName());
        ret.addObject("threadTopic", threadTopic);
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/post/delete", method = RequestMethod.POST)
    public ModelAndView deletePost(HttpServletRequest request, HttpServletResponse response)
    {
        String postIndexRaw = request.getParameter("postIndex");

        if (postIndexRaw == null)
        {
            return errorPage("deletepost_error.html", "MISSING_INDEX", HttpStatus.BAD_REQUEST);
        }

        long postIndex;

        try
        {
            postIndex = Long.valueOf(postIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("deletepost_error.html", "INVALID_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumPost post = postRepository.findByIndex(postIndex);

        if (post == null)
        {
            return errorPage("deletepost_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (post.isDeleted())
        {
            return errorPage("deletepost_error.html", "ALREADY_DELETED", HttpStatus.GONE);
        }

        ForumThread thread = post.getThread();
        ForumUser user = getUserFromRequest(request);
        boolean allowed;

        if (thread == null)
        {
            allowed = userHasPermission(user, UserPermission.MANAGE_DETACHED);
        }
        else
        {
            ForumBoard board = thread.getBoard();
            allowed = board == null ? userHasPermission(user, UserPermission.MANAGE_DETACHED)
                                    : userHasBoardPermission(user, board, BoardPermission.MODERATE);
        }

        if (!allowed)
        {
            return errorPage("deletepost_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String  postUsername = post.getAuthor().getUsername();
        Instant postTime     = post.getPostTime();

        post.deleteContents();

        ModelAndView ret = new ModelAndView("deletepost.html");
        ret.addObject("postUsername", postUsername);
        ret.addObject("postTime", postTime);
        ret.addObject("threadIndex", thread == null ? -1   : thread.getIndex());
        ret.addObject("threadTopic", thread == null ? null : thread.getTopic());
        return ret;
    }
}