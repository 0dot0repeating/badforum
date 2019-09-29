package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumBoardRepository extends JpaRepository<ForumBoard, Long>
{
    @Query("SELECT board FROM ForumBoard board WHERE board.rootBoard = true")
    ForumBoard findRootBoard();
}