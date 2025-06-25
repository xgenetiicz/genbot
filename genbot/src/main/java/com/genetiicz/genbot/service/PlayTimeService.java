package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import com.genetiicz.genbot.database.repository.PlayTimeRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        String sessionKey = userId + ":" + gameName;
        activePlaySessions.put(sessionKey,System.currentTimeMillis());
        System.out.println("Session start: User " + userId + " started playing " + gameName);
    }
    //Method to track end of playtime for the user
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName) {
        //Adding key with value pair that hold userId and gameName, on start and end.
        String sessionKey = userId + ":" + gameName;
        if(!activePlaySessions.containsKey(sessionKey)) {
            return;
        }
        long startTime = activePlaySessions.remove(sessionKey);
        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;
        long durationMinutes= durationMillis / 60000;

        //We want to track at least one minute over since launching the game is not actually playing.
        //if(durationMinutes < 1) {
          //  System.out.println("Session end: User " + userId + " played for less than a minute, not updating.");
          //  return;
        //}
        System.out.println("SESSION END: User " + userId + " played " + gameName + " for " + durationMinutes + " minutes. Updating database.");
        Optional<PlayTimeEntity> recordOpt = playTimeRepository.findByUserIdAndGameName(userId, gameName);

        // Save the record and present it as a new totalTime when playing
        // the same game again, so this will add the current minutes to it.
        recordOpt.ifPresent(record -> {
            long newTotalTime = record.getTotalMinutesPlayed() + durationMinutes;
            record.setTotalMinutesPlayed(newTotalTime);
            playTimeRepository.save(record);
        });
    }

    public long getLiveMinutes(String userId, String gameName) {
        String key = userId + ":" + gameName;
        Long starTime = activePlaySessions.get(key);
        if(starTime == null) {
            return 0;
        }
        return (System.currentTimeMillis() - starTime) / 60000;
    }
    public Optional<Long> getTotalMinutesIncludingLive(String userId, String gameName) {
        long liveMinutes = getLiveMinutes(userId, gameName);
        Optional<PlayTimeEntity> record = playTimeRepository.findByUserIdAndGameNameIgnoreCase(userId, gameName);

        if (record.isPresent()) {
            return Optional.of(record.get().getTotalMinutesPlayed() + liveMinutes);
        } else if (liveMinutes > 0) {
            return Optional.of(liveMinutes);
        } else {
            return Optional.empty();
        }
    }

    @Scheduled(fixedRate = 60000) // runs every 60 seconds
    public void flushActivePlaySessions() {
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : activePlaySessions.entrySet()) {
            String key = entry.getKey(); // format: userId:gameName
            long startTime = entry.getValue();
            long minutesPlayed = (now - startTime) / 60000;

            if (minutesPlayed > 0) {
                String[] parts = key.split(":");
                if (parts.length < 2) {
                    System.out.println("Invalid session key: " + key);
                    continue;
                }

                String userId = parts[0];
                String gameName = parts[1];

                Optional<PlayTimeEntity> recordOpt = playTimeRepository.findByUserIdAndGameName(userId, gameName);
                recordOpt.ifPresent(record -> {
                    record.setTotalMinutesPlayed(record.getTotalMinutesPlayed() + minutesPlayed);
                    playTimeRepository.save(record);
                    System.out.println("Flushed " + minutesPlayed + " minutes for " + userId + " on " + gameName);
                });

                // Reset the session start time to now
                activePlaySessions.put(key, now);
            }
        }
    }
    public List<PlayTimeEntity> get3TopPlayersForGame(String gameName, String serverId) {
        return playTimeRepository.findTop3ByGameNameIgnoreCaseAndServerIdOrderByTotalMinutesPlayedDesc(gameName, serverId);
    }

    // Method here so we can retrieve the array list of matching games for in the slashCommandListener
    public List<String> getMatchingGamesStartingWith(String input, String serverId, int limit) {
        List<String> allGames = playTimeRepository.findDistinctGameNamesByServerId(serverId);
        System.out.println("Fetched games: " + allGames);
        List<String> matches = new ArrayList<>();

        for (String game :  allGames) {
            if(game.toLowerCase().startsWith(input.toLowerCase())) {
                System.out.println("Builded AutoComplete correctly.");
                matches.add(game);
            }
            if (matches.size() >= limit) break;
        }
        return matches;
    }
}
