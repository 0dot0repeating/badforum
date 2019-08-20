package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumRoleRepository extends JpaRepository<ForumRole, Long>
{
    ForumRole findByNameIgnoreCase(String name);
}
