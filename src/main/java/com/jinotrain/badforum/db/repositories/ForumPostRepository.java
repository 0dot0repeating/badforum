package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long>
{
    List<ForumPost> findByAuthorID(long authorID);
    List<ForumPost> findByPostTextIgnoreCaseContaining(String searchText);
}