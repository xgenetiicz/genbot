package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayTimeRepository extends JpaRepository<PlayTimeEntity,Long> {
    Optional<PlayTimeEntity> findByUserIdAndGameName(String userId,String gameName);
    Optional<PlayTimeEntity> findByUserIdAndGameNameIgnoreCase(String userId, String gameName);
    List<PlayTimeEntity> findTop3ByGameNameIgnoreCaseAndServerIdOrderByTotalMinutesPlayedDesc(String gameName, String serverId);
    Optional<PlayTimeEntity> findTopByGameNameIgnoreCaseAndServerId(String gameName, String serverId); //this is for the leaderboard, Not using userId as an parameter
    //Query implementing for finding gameName when typing wrong -- the LevenshteinDistance
    @Query("SELECT DISTINCT p.gameName FROM PlayTimeEntity p WHERE p.serverId = :serverId")
    List<String>findDistinctGameNamesByServerId(String serverId);
    List<PlayTimeEntity> findByUserIdAndServerId(String userId, String serverId);
    //query for finding games that the user has played.
    @Query("SELECT DISTINCT p.gameName FROM PlayTimeEntity p " +
            "WHERE p.serverId = :serverId AND p.userId = :userId " +
            "AND LOWER(p.gameName) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<String> findDistinctUserGameNamesByPrefix(@Param("prefix") String prefix, @Param("serverId") String serverId, @Param("userId") String userId, Pageable pageable);
}
