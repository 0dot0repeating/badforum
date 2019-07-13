package com.jinotrain.badforum.db.repositories;

import com.jinotrain.badforum.db.entities.DBTestDummy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DBTestDummyRepository extends JpaRepository<DBTestDummy, Long>
{
}
