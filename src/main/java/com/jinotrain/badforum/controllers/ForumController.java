package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.PostViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.data.UserViewData;
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

            long postCountLong = em.createNamedQuery("ForumThread.getPostCount", Long.class)
                                   .setParameter("threadID", t.getID())
                                   .getSingleResult();

            int postCount = (int)MiscFuncs.clamp(postCountLong, Integer.MIN_VALUE, Integer.MAX_VALUE);

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


    ThreadViewData getThreadViewData(ForumThread thread, ForumUser viewer, int[] postRange, EntityManager em)
    {
        ForumBoard board = thread.getBoard();

        if (!ForumUser.userHasBoardPermission(viewer, board, BoardPermission.VIEW))
        {
            throw new SecurityException("Viewer is not allowed to view thread's board");
        }

        List<PostViewData> postData = new ArrayList<>();
        List<ForumPost> posts = postRepository.findAllByThreadOrderByPostTime(thread,
                                    new NotDumbPageRequest(postRange[0], postRange[1]));

        long postCountLong = em.createNamedQuery("ForumThread.getPostCount", Long.class)
                                     .setParameter("threadID", thread.getID())
                                     .getSingleResult();

        int postCount = (int)MiscFuncs.clamp(postCountLong, Integer.MIN_VALUE, Integer.MAX_VALUE);

        String viewerUsername = viewer == null ? null : viewer.getUsername();

        boolean hasModeratePrivilege = ForumUser.userHasBoardPermission(viewer, board, BoardPermission.MODERATE);
        boolean hasBanPrivilege      = ForumUser.userHasPermission(viewer, UserPermission.BAN_USERS);

        for (ForumPost p: posts)
        {
            ForumUser user = p.getAuthor();
            String postText = formatPostText(p.getPostText());

            UserViewData userdata = user == null ? new UserViewData()
                                                 : new UserViewData(user.getUsername(), user.isBanned());

            PostViewData pdata = new PostViewData(p.getIndex(), postText, userdata, p.getPostTime(), p.getlastEditTime(),
                                                    p.isDeleted(), p.isUserBanned(), p.getBanReason(), p.getThreadSplitIndex());

            if (user != null && user.getUsername().equalsIgnoreCase(viewerUsername))
            {
                pdata.viewerIsAuthor = true;
            }

            boolean outranks = ForumUser.userOutranksOrIs(viewer, user);
            pdata.canModerate = hasModeratePrivilege && outranks;
            pdata.canBan      = hasBanPrivilege      && outranks;

            postData.add(pdata);
        }

        BoardViewData boardData = board == null ? new BoardViewData(-1, null) : new BoardViewData(board.getIndex(), board.getName());

        ForumUser author = thread.getAuthor();
        UserViewData authorData = author == null ? new UserViewData()
                                                 : new UserViewData(author.getUsername(), author.isBanned());

        int[] displayRange = {postRange[0], postRange[0] + Math.max(postData.size() - 1, 0)};
        ThreadViewData ret = new ThreadViewData(thread.getIndex(), thread.getTopic(), authorData, boardData, postData, postCount, displayRange);
        ret.canModerate = hasModeratePrivilege && ForumUser.userOutranksOrIs(viewer, author);

        return ret;
    }


    static String formatPostText(String postText)
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
}
