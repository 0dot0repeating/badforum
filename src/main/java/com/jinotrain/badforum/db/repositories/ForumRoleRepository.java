package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumRoleRepository extends JpaRepository<ForumRole, Long>
{
    ForumRole findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT role FROM ForumRole role WHERE role.defaultRole = true")
    ForumRole findDefaultRole();
}
