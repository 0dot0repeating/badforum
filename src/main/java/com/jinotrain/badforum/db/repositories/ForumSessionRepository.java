package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumSessionRepository extends JpaRepository<ForumSession, String>
{
    List<ForumSession> findAllByUserOrderByExpireTimeDesc(ForumUser user);
}
