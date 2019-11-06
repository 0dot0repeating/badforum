package com.jinotrain.badforum.controllers;

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
public class CreateRenameDeleteController extends ForumController
{
    @Transactional
    @RequestMapping(value = "/newboard")
    public ModelAndView createNewBoard(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("newboard_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!ForumUser.userHasPermission(user, UserPermission.MANAGE_BOARDS))
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
    @RequestMapping(value = "/deleteboard")
    public ModelAndView deleteBoard(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("deleteboard_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!ForumUser.userHasPermission(user, UserPermission.MANAGE_BOARDS))
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
    @RequestMapping(value = "/deletethread")
    public ModelAndView deleteThread(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("deletethread_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

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
        ForumBoard board   = thread.getBoard();

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (board == null ? !ForumUser.userHasPermission(user, UserPermission.MANAGE_DETACHED)
                          : !ForumUser.userHasBoardPermission(user, board, BoardPermission.MODERATE))
        {
            return errorPage("deletethread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ForumUser author = thread.getAuthor();

        if (!ForumUser.userOutranksOrIs(user, author))
        {
            return errorPage("deletethread_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        String keepPostsRaw = request.getParameter("keepPosts");
        boolean deletePosts = !("true".equalsIgnoreCase(keepPostsRaw) || "1".equals(keepPostsRaw));

        thread.deleteContents(deletePosts, user);

        ModelAndView ret = new ModelAndView("deletethread.html");
        ret.addObject("boardIndex", board == null ? -1   : board.getIndex());
        ret.addObject("boardName",  board == null ? null : board.getName());
        ret.addObject("threadTopic", threadTopic);
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/deletepost")
    public ModelAndView deletePost(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("deletepost_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

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

        if (post.wasSplit())
        {
            return errorPage("deletepost_error.html", "SPLIT_POST", HttpStatus.CONFLICT);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        ForumThread thread = post.getThread();
        ForumUser author   = post.getAuthor();
        boolean allowed    = ForumUser.sameUser(user, author);

        if (!allowed)
        {
            if (thread == null)
            {
                allowed = ForumUser.userHasPermission(user, UserPermission.MANAGE_DETACHED);
            }
            else
            {
                ForumBoard board = thread.getBoard();
                allowed = board == null ? ForumUser.userHasPermission(user, UserPermission.MANAGE_DETACHED)
                                        : ForumUser.userHasBoardPermission(user, board, BoardPermission.MODERATE);
            }
        }

        if (!allowed)
        {
            return errorPage("deletepost_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        if (!ForumUser.userOutranksOrIs(user, author))
        {
            return errorPage("deletepost_error.html", "OUTRANKED", HttpStatus.UNAUTHORIZED);
        }

        String  postUsername = author == null ? "Anonymous" : author.getUsername();
        Instant postTime     = post.getPostTime();

        post.deleteContents();

        ModelAndView ret = new ModelAndView("deletepost.html");
        ret.addObject("postUsername", postUsername);
        ret.addObject("postTime", postTime);
        ret.addObject("threadIndex", thread == null ? -1   : thread.getIndex());
        ret.addObject("threadTopic", thread == null ? null : thread.getTopic());
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/renameboard")
    public ModelAndView renameBoard(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("renameboard_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!ForumUser.userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("renameboard_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String boardIndexRaw = request.getParameter("boardIndex");
        String newName       = request.getParameter("newName");

        if (boardIndexRaw == null || boardIndexRaw.isEmpty())
        {
            return errorPage("renameboard_error.html", "MISSING_INDEX", HttpStatus.BAD_REQUEST);
        }

        if (newName == null || newName.isEmpty())
        {
            return errorPage("renameboard_error.html", "MISSING_NAME", HttpStatus.BAD_REQUEST);
        }

        long boardIndex;

        try
        {
            boardIndex = Long.valueOf(boardIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("renameboard_error.html", "INVALID_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumBoard board = boardRepository.findByIndex(boardIndex);

        if (board == null)
        {
            return errorPage("renameboard_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        String oldName = board.getName();
        board.setName(newName);

        ModelAndView ret = new ModelAndView("renameboard.html");
        ret.addObject("boardIndex", boardIndex);
        ret.addObject("oldName", oldName);
        ret.addObject("newName", newName);
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/renamethread")
    public ModelAndView renameThread(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("renamethread_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        String threadIndexRaw = request.getParameter("threadIndex");
        String newTopic       = request.getParameter("newTopic");

        if (threadIndexRaw == null)
        {
            return errorPage("renamethread_error.html", "MISSING_INDEX", HttpStatus.BAD_REQUEST);
        }

        if (newTopic == null || newTopic.isEmpty())
        {
            return errorPage("renamethread_error.html", "MISSING_NAME", HttpStatus.BAD_REQUEST);
        }

        long threadIndex;

        try
        {
            threadIndex = Long.valueOf(threadIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("renamethread_error.html", "INVALID_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumThread thread = threadRepository.findByIndex(threadIndex);

        if (thread == null)
        {
            return errorPage("renamethread_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (thread.wasMoved())
        {
            return errorPage("renamethread_error.html", "MOVED_THREAD", HttpStatus.CONFLICT);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        ForumUser author = thread.getAuthor();

        if (!ForumUser.userOutranksOrIs(user, author))
        {
            return errorPage("renamethread_error.html", "OUTRANKED", HttpStatus.NOT_FOUND);
        }

        ForumBoard board = thread.getBoard();

        if (board == null ? !ForumUser.userHasPermission(user, UserPermission.MANAGE_DETACHED)
                          : !ForumUser.userHasBoardPermission(user, board, BoardPermission.MODERATE))
        {
            return errorPage("renamethread_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String oldTopic = thread.getTopic();
        thread.setTopic(newTopic);

        ModelAndView ret = new ModelAndView("renamethread.html");
        ret.addObject("threadIndex", threadIndex);
        ret.addObject("oldTopic", oldTopic);
        ret.addObject("newTopic", newTopic);
        ret.addObject("boardIndex", board == null ? -1   : board.getIndex());
        ret.addObject("boardName",  board == null ? null : board.getName());
        return ret;
    }
}
