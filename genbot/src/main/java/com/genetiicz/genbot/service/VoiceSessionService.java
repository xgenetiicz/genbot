package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.VoiceSessionsEntity;
import com.genetiicz.genbot.database.repository.VoiceSessionsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VoiceSessionService {
    private final VoiceSessionsRepository voiceSessionsRepository;
    private final PlayTimeService playTimeService;

    //Inject Repository
    @Autowired
    public VoiceSessionService(VoiceSessionsRepository voiceSessionsRepository,
                               PlayTimeService playTimeService)
    {
        this.voiceSessionsRepository = voiceSessionsRepository;
        this.playTimeService = playTimeService;
    }

    //End the session so we get it stored - call this inside the handleActivityEnd
    public void endSession(String userId, String serverId, String serverName, String gameName)
    {
        voiceSessionsRepository.findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc
                        (userId, serverId, serverName, gameName)
                .ifPresent(session -> {
                    session.setLeaveTime(Instant.now());
                    voiceSessionsRepository.save(session);
                });

    }

    public void handleActivityStart(String userId, String serverId, String serverName, String name)
    {
        //process data to voice_records
        VoiceSessionsEntity voiceRecord = new VoiceSessionsEntity();
        voiceRecord.setUserId(userId);
        voiceRecord.setServerId(serverId);
        voiceRecord.setServerName(serverName);
        voiceRecord.setGameName(voiceRecord.getGameName());

        //save the record
        voiceSessionsRepository.save(voiceRecord);
    }

    //Stop the listener, and we will use endSession here so we save it instant
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName)
    {
        voiceSessionsRepository.findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc
                (userId, serverId, serverName, gameName);
    }
}
