package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayTimeRepository extends JpaRepository<PlayTimeEntity,Long> {
    Optional<PlayTimeEntity> findByUserIdAndGameName(String userId,String gameName);
    Optional<PlayTimeEntity> findByUserIdAndGameNameIgnoreCase(String userId, String gameName);
    List<PlayTimeEntity> findTop3ByGameNameIgnoreCaseAndServerIdOrderByTotalMinutesPlayedDesc(String gameName, String serverId);
}
