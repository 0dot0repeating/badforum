package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodProtectionService;
import com.jinotrain.badforum.data.BoardViewData;
import com.jinotrain.badforum.data.ThreadViewData;
import com.jinotrain.badforum.data.UserViewData;
import com.jinotrain.badforum.db.entities.*;
import com.jinotrain.badforum.db.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
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


    BoardViewData getBoardViewData(ForumBoard board, EntityManager em)
    {
        long boardID = board.getId();

        // get thread and post count, including all child boards

        Collection<ForumBoard> childBoards    = board.getChildBoards();
        List<BoardViewData>    childBoardData = new ArrayList<>();

        long totalThreads = em.createNamedQuery("ForumBoard.getThreadCount", Long.class)
                              .setParameter("boardID", boardID)
                              .getSingleResult();

        long totalPosts   = em.createNamedQuery("ForumBoard.getPostCount", Long.class)
                              .setParameter("boardID", boardID)
                              .getSingleResult();

        for (ForumBoard cb: childBoards)
        {
            long childBoardID = cb.getId();
            Collection<Long> childBoardIDs = new HashSet<>();
            childBoardIDs.add(childBoardID);

            Collection<ForumBoard> curChildBoards = cb.getChildBoards();

            while (!curChildBoards.isEmpty())
            {
                Collection<ForumBoard> newBoards = new HashSet<>();

                for (ForumBoard b: curChildBoards)
                {
                    childBoardIDs.add(b.getId());
                    newBoards.addAll(b.getChildBoards());
                }

                curChildBoards = newBoards;
            }

            long childThreadCount = em.createNamedQuery("ForumBoard.multipleThreadCount", Long.class)
                                      .setParameter("boardID", childBoardIDs)
                                      .getSingleResult();

            long childPostCount = em.createNamedQuery("ForumBoard.multiplePostCount", Long.class)
                                    .setParameter("boardID", childBoardIDs)
                                    .getSingleResult();

            BoardViewData childData = new BoardViewData(childBoardID, cb.getName(), childThreadCount, childPostCount);
            childBoardData.add(childData);

            totalThreads += childThreadCount;
            totalPosts   += childPostCount;
        }

        // for display purposes
        // TODO: implement display ordering in ForumBoard itself
        childBoardData.sort(Comparator.comparing(o -> o.id));

        // gather forum threads, along with post count and thread authors
        // TODO: implement time windows and subranges (will probably require a prodedurally-built query)
        List<ForumThread> threads = em.createNamedQuery("ForumBoard.threadsInUpdateOrder", ForumThread.class)
                                      .setParameter("boardID", boardID)
                                      .setHint("javax.persistence.fetchgraph", em.getEntityGraph("ForumThread.withPosts"))
                                      .getResultList();

        List<ThreadViewData> threadData = new ArrayList<>();

        for (ForumThread t: threads)
        {
            List<ForumPost> posts = new ArrayList<>(t.getPosts());
            long postCount = posts.size();

            UserViewData userdata;

            if (postCount > 0)
            {
                posts.sort(Comparator.comparing(ForumPost::getPostTime));
                ForumPost firstPost = posts.get(0);
                userdata = new UserViewData(firstPost.getAuthor().getUsername());
            }
            else
            {
                userdata = new UserViewData("??????");
            }

            ThreadViewData td = new ThreadViewData(t.getId(), t.getTopic(), userdata, postCount);
            threadData.add(td);
        }

        BoardViewData ret = new BoardViewData(boardID, board.getName(), totalThreads, totalPosts);
        ret.childBoards = childBoardData;
        ret.threads = threadData;

        return ret;
    }
}