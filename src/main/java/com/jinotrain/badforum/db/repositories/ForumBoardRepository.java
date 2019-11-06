package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ForumBoardRepository extends JpaRepository<ForumBoard, Long>
{
    ForumBoard findByIndex(long index);

    @Query("SELECT board FROM ForumBoard board WHERE board.parentBoard.index IN :indexes")
    List<ForumBoard> findAllByParentIndexIn(@Param("indexes") Collection<Long> indexes);

    @Query("SELECT board FROM ForumBoard board WHERE board.rootBoard = true")
    ForumBoard findRootBoard();

    @Query("SELECT COALESCE(MAX(e.index), 0) from ForumBoard e")
    long getHighestIndex();
}
