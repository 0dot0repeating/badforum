package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.data.*;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.*;
import com.jinotrain.badforum.db.repositories.*;
import com.jinotrain.badforum.util.DurationFormat;
import com.jinotrain.badforum.util.MiscFuncs;
import com.jinotrain.badforum.util.NotDumbPageRequest;
import com.jinotrain.badforum.util.UserBannedException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

abstract class ForumController
{
    @Autowired
    FloodProtectionService floodProtectionService;

    @Autowired
    ForumSessionRepository sessionRepository;

    @Autowired
    ForumUserRepository userRepository;

    @Autowired
    ForumPreferencesRepository prefsRepository;

    @Autowired
    ForumRoleRepository roleRepository;

    @Autowired
    ForumBoardRepository boardRepository;

    @Autowired
    ForumThreadRepository threadRepository;

    @Autowired
    ForumPostRepository postRepository;

    @PersistenceContext
    EntityManager em;


    static Parser markdownParser = Parser.builder().build();
    static HtmlRenderer mdToHTML = HtmlRenderer.builder().escapeHtml(true).build();


    ForumSession getForumSession(HttpServletRequest request)
    {
        return getForumSession(request, false);
    }

    ForumSession getForumSession(HttpServletRequest request, boolean refresh)
    {
        Object possibleSession = request.getAttribute("forumSession");

        if (possibleSession instanceof ForumSession)
        {
            ForumSession session = (ForumSession)possibleSession;
            if (refresh) { session = sessionRepository.findById(session.getId()).orElse(null); }
            return session;
        }

        return null;
    }


    ForumUser getUserFromRequest(HttpServletRequest request) throws UserBannedException
    {
        ForumSession session = getForumSession(request);
        if (session == null) { return null; }

        ForumUser user = session.getUser();

        if (user != null)
        {
            if (user.isBanned())
            {
                throw new UserBannedException(user.getBanReason(), user.getBannedUntil(), session.getId());
            }

            user = userRepository.findByUsernameIgnoreCase(user.getUsername());
        }

        return user;
    }


    private List<BoardViewData> getChildBoardViewData(Collection<ForumBoard> childBoards, ForumUser viewer, EntityManager em)
    {
        List<BoardViewData> ret = new ArrayList<>();

        for (ForumBoard cb: childBoards)
        {
            if (!ForumUser.userHasBoardPermission(viewer, cb, BoardPermission.VIEW)) { continue; }

            long childBoardID = cb.getId();
            Collection<Long> childBoardIDs = new HashSet<>();
            childBoardIDs.add(childBoardID);

            Collection<ForumBoard> curChildBoards = cb.getChildBoards();

            while (!curChildBoards.isEmpty())
            {
                Collection<ForumBoard> newBoards = new HashSet<>();

                for (ForumBoard b: curChildBoards)
                {
                    if (ForumUser.userHasBoardPermission(viewer, b, BoardPermission.VIEW))
                    {
                        childBoardIDs.add(b.getId());
                        newBoards.addAll(b.getChildBoards());
                    }
                }

                curChildBoards = newBoards;
            }

            long childThreadCount = em.createNamedQuery("ForumBoard.multipleThreadCount", Long.class)
                                      .setParameter("boardIDs", childBoardIDs)
                                      .getSingleResult();

            long childPostCount = em.createNamedQuery("ForumBoard.multiplePostCount", Long.class)
                                    .setParameter("boardIDs", childBoardIDs)
                                    .getSingleResult();

            BoardViewData childData = new BoardViewData(cb.getIndex(), cb.getName(), childThreadCount, childPostCount);
            ret.add(childData);
        }

        // TODO: implement some way to set child board order
        ret.sort(Comparator.comparing(o -> o.index));
        return ret;
    }


    BoardViewData getBoardViewData(ForumBoard board, ForumUser viewer, int[] threadRange, EntityManager em)
    {
        if (!ForumUser.userHasBoardPermission(viewer, board, BoardPermission.VIEW))
        {
            throw new SecurityException("Viewer is not allowed to view board");
        }

        List<BoardViewData>  childBoardData = getChildBoardViewData(board.getChildBoards(), viewer, em);
        List<ThreadViewData> threadData     = new ArrayList<>();

        List<ForumThread> threads = threadRepository.findAllByBoardOrderByLastUpdateDesc(board,
                                        new NotDumbPageRequest(threadRange[0], threadRange[1]));

        boolean hasModeratePrivilege = ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE);

        for (ForumThread t: threads)
        {
            boolean wasMoved = false;

            while (t.wasMoved())
            {
                long moveIndex = t.getMoveIndex();
                t = threadRepository.findByIndex(moveIndex);
                if (t == null) { break; }

                wasMoved = true;
            }

            if (t == null || t.isDeleted()) { continue; }

            long postCountLong = postRepository.countByThread(t);
            int  postCount     = (int)MiscFuncs.clamp(postCountLong, Integer.MIN_VALUE, Integer.MAX_VALUE);

            Instant creationTime = t.getCreationTime();
            Instant lastUpdate   = t.getLastUpdate();
            ForumUser author     = t.getAuthor();

            UserViewData userdata = author == null ? new UserViewData()
                                                   : new UserViewData(author.getUsername(), author.isBanned());

            ThreadViewData td = new ThreadViewData(t.getIndex(), t.getTopic(), userdata, postCount, creationTime, lastUpdate, wasMoved);
            td.canModerate = hasModeratePrivilege && ForumUser.userOutranksOrIs(viewer, author);
            threadData.add(td);
        }

        long totalThreads = threadRepository.countByBoard(board);
        long totalPosts   = em.createNamedQuery("ForumBoard.getPostCount", Long.class)
                              .setParameter("boardID", board.getId())
                              .getSingleResult();

        int[] displayRange = {threadRange[0], threadRange[0] + Math.max(0, threads.size() - 1)};
        BoardViewData ret = new BoardViewData(board.getIndex(), board.getName(), totalThreads, totalPosts,
                                                board.isRootBoard(), displayRange);

        ret.canManage = ForumUser.userHasPermission(viewer, UserPermission.MANAGE_BOARDS)
                     && ForumUser.userOutranksOrIs(viewer, board.getCreator());
        ret.childBoards = childBoardData;
        ret.threads = threadData;

        return ret;
    }


    ThreadViewData getThreadViewData(ForumThread thread, ForumUser viewer, int[] postRange)
    {
        ForumBoard board = thread.getBoard();

        if (board != null && !ForumUser.userHasBoardPermission(viewer, board, BoardPermission.VIEW))
        {
            throw new SecurityException("Viewer is not allowed to view thread's board");
        }

        List<PostViewData> postData = new ArrayList<>();
        List<ForumPost>    posts    = postRepository.findAllByThreadOrderByPostTime(thread,
                                          new NotDumbPageRequest(postRange[0], postRange[1]));

        long postCountLong = postRepository.countByThread(thread);
        int postCount = (int)MiscFuncs.clamp(postCountLong, Integer.MIN_VALUE, Integer.MAX_VALUE);

        boolean canBan      = ForumUser.userHasPermission(viewer, UserPermission.BAN_USERS);
        boolean canModerate = board == null ? ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                                            : ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE);

        for (ForumPost p: posts)
        {
            postData.add(getPostViewData(p, viewer, canModerate, canBan));
        }

        BoardViewData boardData = board == null ? new BoardViewData(-1, null) : new BoardViewData(board.getIndex(), board.getName());

        ForumUser author = thread.getAuthor();
        UserViewData authorData = author == null ? new UserViewData()
                                                 : new UserViewData(author.getUsername(), author.isBanned());

        int[] displayRange = {postRange[0], postRange[0] + Math.max(postData.size() - 1, 0)};
        ThreadViewData ret = new ThreadViewData(thread.getIndex(), thread.getTopic(), authorData, boardData, postData, postCount, displayRange);
        ret.canModerate = canModerate && ForumUser.userOutranksOrIs(viewer, author);

        return ret;
    }


    PostViewData getPostViewData(ForumPost post, ForumUser viewer)
    {
        ForumThread postThread = post.getThread();
        ForumBoard  postBoard  = postThread == null ? null : postThread.getBoard();

        boolean canBan      = ForumUser.userHasPermission(viewer, UserPermission.BAN_USERS);
        boolean canModerate = postBoard == null ? ForumUser.userHasPermission(viewer, UserPermission.MANAGE_DETACHED)
                                                : ForumUser.userHasBoardPermission(viewer, postBoard, BoardPermission.MODERATE);

        return getPostViewData(post, viewer, canModerate, canBan);
    }


    PostViewData getPostViewData(ForumPost post, ForumUser viewer, boolean canModerate, boolean canBan)
    {
        ForumUser poster = post.getAuthor();
        String postText = formatPostText(post.getPostText());

        UserViewData userdata = poster == null ? new UserViewData()
                                               : new UserViewData(poster.getUsername(), poster.isBanned());

        PostViewData pdata = new PostViewData(post.getIndex(), postText, userdata, post.getPostTime(), post.getlastEditTime(),
                post.isDeleted(), post.isUserBanned(), post.getBanReason(), post.getThreadSplitIndex());

        boolean outranks = ForumUser.userOutranksOrIs(viewer, poster);
        pdata.viewerIsAuthor = ForumUser.sameUser(poster, viewer);
        pdata.canModerate    = canModerate && outranks;
        pdata.canBan         = canBan      && outranks;
        return pdata;
    }


    String formatPostText(String postText)
    {
        Node parsedPostText = markdownParser.parse(postText);
        return mdToHTML.render(parsedPostText);
    }


    ModelAndView errorPage(String viewName, String errorCode, HttpStatus status)
    {
        ModelAndView errorMAV = new ModelAndView(viewName);
        errorMAV.setStatus(status);
        errorMAV.addObject("errorCode", errorCode);
        return errorMAV;
    }


    ModelAndView bannedPage(UserBannedException e)
    {
        ForumSession session = sessionRepository.findById(e.getSessionID()).orElse(null);

        if (session != null)
        {
            session.setRefreshDuration(Duration.ofMillis(250));
            session.refreshExpireTime();
        }

        ModelAndView bannedMAV = new ModelAndView("banned.html");
        bannedMAV.setStatus(HttpStatus.UNAUTHORIZED);
        bannedMAV.addObject("banReason", e.getBanReason());
        bannedMAV.addObject("bannedUntil", e.getBannedUntil());
        return bannedMAV;
    }


    ModelAndView floodingPage(FloodCategory category)
    {
        ModelAndView mav = new ModelAndView("flooding.html");
        Duration floodWindow = floodProtectionService.getFloodWindow(category);

        mav.setStatus(HttpStatus.TOO_MANY_REQUESTS);
        mav.addObject("floodType", category.niceName);
        mav.addObject("floodWindow", DurationFormat.format(floodWindow));
        return mav;
    }


    List<LinkViewData> getForumPath(ForumBoard board, ForumUser viewer)
    {
        List<ForumBoard> boards = new ArrayList<>();
        ForumBoard currentBoard = board;

        while (currentBoard != null)
        {
            if (ForumUser.userHasBoardPermission(viewer, currentBoard, BoardPermission.VIEW))
            {
                boards.add(currentBoard);
            }

            currentBoard = currentBoard.getParentBoard();
        }

        ListIterator<ForumBoard> it = boards.listIterator(boards.size());
        List<LinkViewData> boardLinks = new ArrayList<>();

        while (it.hasPrevious())
        {
            ForumBoard nextBoard = it.previous();
            LinkViewData boardLink = new LinkViewData("/board/" + nextBoard.getIndex(), nextBoard.getName());
            boardLinks.add(boardLink);
        }

        return boardLinks;
    }


    List<LinkViewData> getForumPath(ForumThread thread, ForumUser viewer)
    {
        if (thread == null) { return new ArrayList<>(); }

        LinkViewData threadLink = new LinkViewData("/thread/" + thread.getIndex(), thread.getTopic());

        List<LinkViewData> boardLinks = getForumPath(thread.getBoard(), viewer);
        boardLinks.add(threadLink);
        return boardLinks;
    }


    List<LinkViewData> getForumPath(ForumPost post, ForumUser viewer)
    {
        if (post == null) { return new ArrayList<>(); }

        long postIndex = post.getIndex();
        LinkViewData postLink = new LinkViewData("/singlepost/" + postIndex, "Post #" + postIndex);

        List<LinkViewData> threadLinks = getForumPath(post.getThread(), viewer);
        threadLinks.add(postLink);
        return threadLinks;
    }
}
