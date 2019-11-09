package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long>
{
    List<ForumPost> findByAuthor(ForumUser author);
    List<ForumPost> findByPostTextIgnoreCaseContaining(String searchText);
    List<ForumPost> findAllByThreadOrderByPostTime(ForumThread thread, Pageable page);

    ForumPost findByIndex(long index);

    @Query("SELECT COALESCE(MAX(e.index), 0) from ForumPost e")
    long getHighestIndex();
}
