package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayTimeRepository extends JpaRepository<PlayTimeEntity, Long> {

    Optional<PlayTimeEntity> findByUserIdAndGameNameIgnoreCase(String userId, String gameName);

    @Query("""
  SELECT g FROM PlayTimeEntity g
  JOIN PlayTimeServerEntity m
    ON g.userId = m.userId AND g.gameName = m.gameName
  WHERE UPPER(g.gameName) = UPPER(:gameName)
    AND m.serverId = :serverId
  ORDER BY g.totalMinutesPlayed DESC
""")
    List<PlayTimeEntity> findTop3ByGameAndServerMapping(
            @Param("gameName") String gameName,
            @Param("serverId")   String serverId,
            Pageable pageable
    );


    // Query implementing for finding gameName when typing wrong — Levenshtein fallback next
    @Query("SELECT DISTINCT p.gameName FROM PlayTimeEntity p WHERE p.serverId = :serverId")
    List<String> findDistinctGameNamesByServerId(@Param("serverId") String serverId);

    List<PlayTimeEntity> findByUserIdAndServerId(String userId, String serverId);

    // Autocomplete for /myplaytime: games this user has played on that server
    @Query("""
        SELECT DISTINCT p.gameName
        FROM PlayTimeEntity p
        WHERE p.serverId = :serverId
          AND p.userId = :userId
          AND LOWER(p.gameName) LIKE LOWER(CONCAT(:prefix, '%'))
    """)
    List<String> findDistinctUserGameNamesByPrefix(
            @Param("prefix") String prefix,
            @Param("serverId") String serverId,
            @Param("userId") String userId,
            Pageable pageable
    );

    // Autocomplete strict: all games this user has on that server
    @Query("SELECT DISTINCT p.gameName FROM PlayTimeEntity p WHERE p.userId = :userId AND p.serverId = :serverId")
    List<String> findDistinctGameNamesByUserIdAndServerId(
            @Param("userId") String userId,
            @Param("serverId") String serverId
    );

    //fetching friend's specific game entry on the server both are in
    Optional<PlayTimeEntity> findByUserIdAndGameNameAndServerId (String userId, String gameName, String serverId);
}
