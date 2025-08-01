package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeServerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayTimeServerRepository extends JpaRepository<PlayTimeServerEntity, Long> {

    Optional<PlayTimeServerEntity> findByUserIdAndGameNameAndServerIdIgnoreCase(
            String userId,
            String gameName,
            String serverId
    );

    @Query("SELECT DISTINCT p.gameName FROM PlayTimeServerEntity p WHERE p.serverId = :serverId")
    List<String> findDistinctGameNamesByServerId(String serverId);

    @Query("""
        SELECT DISTINCT p.gameName
        FROM PlayTimeServerEntity p
        WHERE LOWER(p.gameName) LIKE LOWER(CONCAT(:prefix, '%'))
          AND p.userId = :userId
          AND p.serverId = :serverId
    """)
    List<String> findDistinctGameNamesByUserIdAndServerIdIgnoreCase(
            @Param("prefix") String prefix,
            @Param("serverId") String serverId,
            @Param("userId") String userId,
            Pageable pageable
    );

    List<PlayTimeServerEntity> findByUserIdAndServerId(String userId, String serverId);
}
