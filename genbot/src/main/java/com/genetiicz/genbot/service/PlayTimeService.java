package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import com.genetiicz.genbot.database.repository.PlayTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class PlayTimeService {
    private final PlayTimeRepository playTimeRepository;
    private final Map<String, Long> activePlaySessions = new ConcurrentHashMap<>();

    //Using Autowired to automatically inject repository using constructor
    @Autowired
    public PlayTimeService(PlayTimeRepository playTimeRepository) {
        this.playTimeRepository = playTimeRepository;
    }

    //Method to track playtime by handling activity start and end.
    public void handleActivityStart(String userId, String serverId, String serverName, String gameName) {
        //Use the repository first to check if user have already record.
        Optional<PlayTimeEntity> existingRecordOpt = playTimeRepository.findByUserIdAndGameName(userId,gameName);
        //Check if user already have a record
        if(existingRecordOpt.isEmpty()) {
            System.out.println("First time playing this game! Creating a new record for you! :)");


        //We make a new record if a user doesn't have any record
        //Create a new instance of the object with new record
        PlayTimeEntity newRecord = new PlayTimeEntity();
        newRecord.setUserId(userId);
        newRecord.setServerId(serverId);
        newRecord.setServerName(serverName);
        newRecord.setGameName(gameName);
        newRecord.setTotalMinutesPlayed(0);

        //Save the record
        playTimeRepository.save(newRecord);
        }
        //Now we start the sessionTimer for trackings the User's
        //Activity on game
        activePlaySessions.put(userId,System.currentTimeMillis());
        System.out.println("Session start: User " + userId + " started playing " + gameName);
    }
    //Method to track end of playtime for the user
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName) {
        if(!activePlaySessions.containsKey(userId)) {
            return;
        }
        long startTime = activePlaySessions.remove(userId);
        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;
        long durationMinutes= durationMillis / 60000;

        if(durationMinutes < 1) {
            System.out.println("Session end: User " + userId + " played for less than a minute, not updating.");
            return;
        }
        System.out.println("SESSION END: User " + userId + " played " + gameName + " for " + durationMinutes + " minutes. Updating database.");
        Optional<PlayTimeEntity> recordOpt = playTimeRepository.findByUserIdAndGameName(userId, gameName);

        // Save the record.
        recordOpt.ifPresent(record -> {
            long newTotalTime = record.getTotalMinutesPlayed() + durationMinutes;
            record.setTotalMinutesPlayed(newTotalTime);
            playTimeRepository.save(record);
        });
    }
}
