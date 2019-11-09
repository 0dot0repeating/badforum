package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ForumThreadRepository extends JpaRepository<ForumThread, Long>
{
    ForumThread findByIndex(long index);

    List<ForumThread> findAllByBoardOrderByLastUpdateDesc(ForumBoard board);
    List<ForumThread> findAllByBoardOrderByLastUpdateDesc(ForumBoard board, Pageable page);
    long countByBoard(ForumBoard board);

    @Query("SELECT COALESCE(MAX(e.index), 0) from ForumThread e")
    long getHighestIndex();
}
