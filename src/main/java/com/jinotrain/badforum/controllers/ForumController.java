package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.PostViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.data.UserViewData;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.*;
import com.jinotrain.badforum.db.repositories.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
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


    ForumUser getUserFromRequest(HttpServletRequest request)
    {
        ForumSession session = getForumSession(request);
        if (session == null) { return null; }

        ForumUser user = session.getUser();
        if (user != null) { user = userRepository.findByUsernameIgnoreCase(user.getUsername()); }
        return user;
    }


    BoardViewData getBoardViewData(ForumBoard board, ForumUser viewer, EntityManager em)
    {
        if (!userHasBoardPermission(viewer, board, BoardPermission.VIEW))
        {
            throw new SecurityException("Viewer is not allowed to view board");
        }

        long boardID = board.getId();

        // get thread and post count, including all child boards

        Collection<ForumBoard> childBoards    = board.getChildBoards();
        List<BoardViewData>    childBoardData = new ArrayList<>();

        // gather forum threads, along with post count and thread authors
        // TODO: implement time windows and subranges (will probably require a prodedurally-built query)
        List<ForumThread> threads = em.createNamedQuery("ForumBoard.threadsInUpdateOrder", ForumThread.class)
                                            .setParameter("boardID", boardID)
                                            .setHint("javax.persistence.fetchgraph", em.getEntityGraph("ForumThread.withPosts"))
                                            .getResultList();

        long totalThreads = threads.size();
        long totalPosts   = em.createNamedQuery("ForumBoard.getPostCount", Long.class)
                              .setParameter("boardID", boardID)
                              .getSingleResult();

        for (ForumBoard cb: childBoards)
        {
            if (!userHasBoardPermission(viewer, cb, BoardPermission.VIEW)) { continue; }

            long childBoardID = cb.getId();
            Collection<Long> childBoardIDs = new HashSet<>();
            childBoardIDs.add(childBoardID);

            Collection<ForumBoard> curChildBoards = cb.getChildBoards();

            while (!curChildBoards.isEmpty())
            {
                Collection<ForumBoard> newBoards = new HashSet<>();

                for (ForumBoard b: curChildBoards)
                {
                    if (userHasBoardPermission(viewer, b, BoardPermission.VIEW))
                    {
                        childBoardIDs.add(b.getId());
                        newBoards.addAll(b.getChildBoards());
                    }
                }

                curChildBoards = newBoards;
            }

            long childThreadCount = em.createNamedQuery("ForumBoard.multipleThreadCount", Long.class)
                                      .setParameter("boardID", childBoardIDs)
                                      .getSingleResult();

            long childPostCount = em.createNamedQuery("ForumBoard.multiplePostCount", Long.class)
                                    .setParameter("boardID", childBoardIDs)
                                    .getSingleResult();

            BoardViewData childData = new BoardViewData(cb.getIndex(), cb.getName(), childThreadCount, childPostCount);
            childBoardData.add(childData);

            totalThreads += childThreadCount;
            totalPosts   += childPostCount;
        }

        // for display purposes
        // TODO: implement some way to specify order key
        childBoardData.sort(Comparator.comparing(o -> o.id));

        List<ThreadViewData> threadData = new ArrayList<>();

        for (ForumThread t: threads)
        {
            List<ForumPost> posts = new ArrayList<>(t.getPosts());
            int postCount = posts.size();

            UserViewData userdata;
            Instant creationTime = null;
            Instant lastUpdate   = null;

            if (postCount > 0)
            {
                posts.sort(Comparator.comparing(ForumPost::getPostTime));
                ForumPost firstPost = posts.get(0);
                ForumPost lastPost  = posts.get(postCount-1);

                ForumUser userAuthor = firstPost.getAuthor();
                userdata = new UserViewData(userAuthor == null ? null : userAuthor.getUsername());

                creationTime = firstPost.getPostTime();
                lastUpdate   = lastPost.getPostTime();
            }
            else
            {
                userdata = new UserViewData(null);
            }

            ThreadViewData td = new ThreadViewData(t.getIndex(), t.getTopic(), userdata, postCount, creationTime, lastUpdate);
            threadData.add(td);
        }

        BoardViewData ret = new BoardViewData(boardID, board.getName(), totalThreads, totalPosts);
        ret.childBoards = childBoardData;
        ret.threads = threadData;

        return ret;
    }


    ThreadViewData getThreadViewData(ForumThread thread, ForumUser viewer, EntityManager em)
    {
        ForumBoard board = thread.getBoard();

        if (!userHasBoardPermission(viewer, board, BoardPermission.VIEW))
        {
            throw new SecurityException("Viewer is not allowed to view thread's board");
        }

        Collection<ForumPost> posts = thread.getPosts();
        List<PostViewData> postData = new ArrayList<>();
        UserViewData firstPoster = null;

        for (ForumPost p: posts)
        {
            ForumUser user = p.getAuthor();
            UserViewData userdata = new UserViewData(user == null ? null : user.getUsername());
            String postText = formatPostText(p.getPostText());

            PostViewData pdata = new PostViewData(p.getIndex(), postText, userdata, p.getPostTime(), p.getlastEditTime());
            postData.add(pdata);

            if (firstPoster == null) { firstPoster = userdata; }
        }

        BoardViewData boardData = board == null ? new BoardViewData(-1, null) : new BoardViewData(board.getIndex(), board.getName());

        return new ThreadViewData(thread.getID(), thread.getTopic(), firstPoster, boardData, postData);
    }


    static String formatPostText(String postText)
    {
        Node parsedPostText = markdownParser.parse(postText);
        return mdToHTML.render(parsedPostText);
    }


    boolean userHasPermission(ForumUser user, UserPermission permission)
    {
        if (user == null) { return false; }
        return user.hasPermission(permission);
    }


    boolean userHasBoardPermission(ForumUser user, ForumBoard board, BoardPermission permission)
    {
        if (user == null) { return board.getGlobalPermission(permission); }
        return user.hasBoardPermission(board, permission);
    }
}
