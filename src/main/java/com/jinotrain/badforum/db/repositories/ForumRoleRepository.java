package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ForumRoleRepository extends JpaRepository<ForumRole, Long>
{
    ForumRole findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    Collection<ForumRole> findAllByAdmin(boolean admin);

    @Query("SELECT role FROM ForumRole role WHERE LOWER(role.name) IN :roleNames")
    List<ForumRole> findAllByNameIgnoreCaseIn(@Param("roleNames") Collection<String> names);

    @Query("SELECT role FROM ForumRole role WHERE role.defaultRole = true")
    ForumRole findDefaultRole();
}
