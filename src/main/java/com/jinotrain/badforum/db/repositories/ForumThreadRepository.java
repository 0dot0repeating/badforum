package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>
{
}
