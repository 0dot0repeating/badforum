package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumUserRepository extends JpaRepository<ForumUser, UUID>
{
    Optional<ForumUser> findByUsernameIgnoreCase(String username);
}
