package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import com.genetiicz.genbot.database.entity.VoiceSessionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface VoiceSessionsRepository extends JpaRepository<VoiceSessionsEntity, Long> {
    Optional<VoiceSessionsEntity> findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc
            (String userId, String serverId, String serverName,String gameName);
}
