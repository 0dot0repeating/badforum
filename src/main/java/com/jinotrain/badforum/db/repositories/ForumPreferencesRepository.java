package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.ForumPreferences;
import com.jinotrain.badforum.db.entities.ForumUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPreferencesRepository extends JpaRepository<ForumPreferences, Long>
{
    ForumPreferences findByUser(ForumUser user);
}
