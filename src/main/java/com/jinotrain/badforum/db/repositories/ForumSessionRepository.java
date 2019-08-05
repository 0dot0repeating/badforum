package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumSessionRepository extends JpaRepository<ForumSession, String>
{
}
