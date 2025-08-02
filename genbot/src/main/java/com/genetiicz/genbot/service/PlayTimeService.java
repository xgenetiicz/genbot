package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import com.genetiicz.genbot.database.entity.PlayTimeServerEntity;
import com.genetiicz.genbot.database.repository.PlayTimeRepository;
import com.genetiicz.genbot.database.repository.PlayTimeServerRepository;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Service
public class PlayTimeService {
    private final PlayTimeRepository playTimeRepository;
    private final PlayTimeServerRepository playTimeServerRepository;
    private final Map<String, Long> activePlaySessions = new ConcurrentHashMap<>();

    //Using Autowired to automatically inject repositories via constructor
    @Autowired
    public PlayTimeService(PlayTimeRepository playTimeRepository,
                           PlayTimeServerRepository playTimeServerRepository) {
        this.playTimeRepository = playTimeRepository;
        this.playTimeServerRepository = playTimeServerRepository;
    }

    //Method to track playtime by handling activity start and end.
    public String handleActivityStart(String userId, String serverId, String serverName, String gameName) {
        //Use the repository first to check if user have already a global record.
        Optional<PlayTimeEntity> existingGlobal = playTimeRepository.findByUserIdAndGameNameIgnoreCase(userId, gameName);
        //Check if user already have a record
        if (existingGlobal.isEmpty()) {
            System.out.println("First time playing this game! Creating a new record for you! :)");
            //We make a new global record if a user doesn't have any record
            //Create a new instance of the object with new record
            PlayTimeEntity newRecord = new PlayTimeEntity();
            newRecord.setUserId(userId);
            newRecord.setServerId(serverId);
            newRecord.setGameName(gameName);
            newRecord.setTotalMinutesPlayed(0);
            //Save the record
            playTimeRepository.save(newRecord);
        }

        //Use the server-log repository to check if mapping exists
        boolean mappingExists = playTimeServerRepository
                .findByUserIdAndGameNameAndServerIdIgnoreCase(userId, gameName, serverId)
                .isPresent();
        if (!mappingExists) {
            //Create server mapping if not present
            PlayTimeServerEntity map = new PlayTimeServerEntity();
            map.setUserId(userId);
            map.setGameName(gameName);
            map.setServerId(serverId);
            map.setServerName(serverName);
            playTimeServerRepository.save(map);
        }

        //Now we start the sessionTimer for tracking the User's
        //Activity on game
        String sessionKey = userId + ":" + serverId + ":" + gameName;
        activePlaySessions.put(sessionKey, System.currentTimeMillis());
        System.out.println("Session start: User " + userId + " started playing " + gameName + " in server: **" + serverId + " **");

        // return welcome for new-to-server only
        return mappingExists ? null : "First time playing this **" + gameName + "** on this server! Welcome!";
    }

    //Method to track end of playtime for the user
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName) {
        //Adding key with value pair that hold userId and gameName and serverId
        String sessionKey = userId + ":" + serverId + ":" + gameName;
        if (!activePlaySessions.containsKey(sessionKey)) {
            return;
        }
        long startTime = activePlaySessions.remove(sessionKey);
        long durationMillis = System.currentTimeMillis() - startTime;
        long durationMinutes= durationMillis / 60000;

        //We want to track at least one minute over since launching the game is not actually playing.
        if (durationMinutes < 1) {
            System.out.println("Session end: User " + userId + " played for less than a minute, not updating.");
            return;
        }
        System.out.println("SESSION END: User " + userId + " played " + gameName + " for " + durationMinutes + " minutes. Updating database.");

        // Update global total
        playTimeRepository.findByUserIdAndGameNameIgnoreCase(userId, gameName)
                .ifPresent(record -> {
                    record.setTotalMinutesPlayed(record.getTotalMinutesPlayed() + durationMinutes);
                    playTimeRepository.save(record);
                });
    }

    public long getLiveMinutes(String userId, String gameName) {
        String key = userId + ":" + gameName;
        Long startTime = activePlaySessions.get(key);
        if (startTime == null) {
            return 0;
        }
        return (System.currentTimeMillis() - startTime) / 60000;
    }

    public Optional<Long> getTotalMinutesIncludingLive(String userId, String gameName) {
        long liveMinutes = getLiveMinutes(userId,gameName);
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
            String key = entry.getKey(); // format: userId:serverId:gameName
            long startTime = entry.getValue();
            long minutesPlayed = (now - startTime) / 60000;

            if (minutesPlayed > 0) {
                String[] parts = key.split(":", 3);
                if (parts.length < 3) {
                    System.out.println("Invalid session key: " + key);
                    continue;
                }

                String userId = parts[0];
                String sid = parts[1];
                String gm = parts[2];

                Optional<PlayTimeEntity> recordOpt = playTimeRepository.findByUserIdAndGameNameIgnoreCase(userId, gm);
                recordOpt.ifPresent(record -> {
                    record.setTotalMinutesPlayed(record.getTotalMinutesPlayed() + minutesPlayed);
                    playTimeRepository.save(record);
                    System.out.println("Flushed " + minutesPlayed + " minutes for " + userId + " on " + gm + " in " + sid);
                });

                // Reset the session start time to now
                activePlaySessions.put(key, now);
            }
        }
    }

    public List<PlayTimeEntity> get3TopPlayersForGame(String gameName, String serverId) {
        return playTimeRepository.findTop3ByGameAndServerMapping(gameName, serverId, PageRequest.of(0,3)); //This mapping will just include leaderboards that have tracking -  and games.
    }

    // Method here so we can retrieve the array list of matching games for in the slashCommandListener
    public List<String> getMatchingGamesStartingWith(String input, String serverId, int limit) {
        List<String> allGames = playTimeServerRepository.findDistinctGameNamesByServerId(serverId);

        System.out.println("Fetched games: " + allGames);
        return allGames.stream()
                .filter(game -> game.toLowerCase().startsWith(input.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    //Method to retrive the list but for /myplaytime it should only retrieve an array list
    //that the user has played, not the actual leaderboard list.
    public List<PlayTimeEntity> getGamesPlayedByUser(String userId, String serverId) {
        return playTimeServerRepository.findByUserIdAndServerId(userId, serverId)
                .stream()
                .map(m -> playTimeRepository.findByUserIdAndGameNameIgnoreCase(userId, m.getGameName()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //suggest autocompletion for the user based on input.
    public List<String> getMatchingGamesForUserStartingWith(String input, String serverId, String userId, int limit) {
        return playTimeServerRepository
                .findDistinctGameNamesByUserIdAndServerIdIgnoreCase(input, serverId, userId, PageRequest.of(0, limit));
    }

    //Find the friends global playtime for the game - or empty if nothing
    public Optional<Long> getFriendTotalMinutes (String friendId, String gameName, String serverId) {
        return playTimeRepository.findByUserIdAndGameNameAndServerId(friendId, gameName,serverId).map(PlayTimeEntity::getTotalMinutesPlayed);
    }
}
