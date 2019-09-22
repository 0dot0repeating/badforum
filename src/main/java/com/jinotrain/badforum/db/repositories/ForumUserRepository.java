package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumUserRepository extends JpaRepository<ForumUser, Long>
{
    ForumUser findByUsernameIgnoreCase(String username);
}
