package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>
{
    ForumThread findByIndex(long index);

    @Query("SELECT COALESCE(MAX(e.index), 0) from ForumThread e")
    long getHighestIndex();
}
