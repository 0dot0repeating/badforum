package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface ForumSessionRepository extends JpaRepository<ForumSession, String>
{
    int MAX_SESSIONS_PER_USER = 16;

    List<ForumSession> findAllByUserOrderByLastUseTimeDesc(ForumUser user);


    default List<String> pruneSessions(ForumUser user)
    {
        List<String> prunedIDs = new ArrayList<>();

        List<ForumSession> userSessions = findAllByUserOrderByLastUseTimeDesc(user);
        Instant now = Instant.now();

        int sessionCount = 0;

        for (ForumSession session: userSessions)
        {
            if (sessionCount >= MAX_SESSIONS_PER_USER || session.getExpireTime().isBefore(now))
            {
                delete(session);
                prunedIDs.add(session.getId());
            }
            else
            {
                sessionCount += 1;
            }
        }

        return prunedIDs;
    }
}
