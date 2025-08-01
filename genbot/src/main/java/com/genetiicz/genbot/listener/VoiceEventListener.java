package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.VoiceSessionService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoiceEventListener extends ListenerAdapter {
    private final VoiceSessionService voiceSessionService;
    //the idea is only to track now when they are in voice in the particual server // moved this to here since we will store
    // in a new entity
    private final Map<String,String> inVoice = new ConcurrentHashMap<>();

    @Autowired
    public VoiceEventListener(VoiceSessionService voiceSessionService){
        this.voiceSessionService =  voiceSessionService;
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return; //check bot

        String userId = event.getMember().getId();
        String serverId = event.getGuild().getId();
        // user just joined a voice channel
        if (event.getChannelJoined() != null) {
            System.out.println(userId + " Joined voice in guild " + serverId);
            inVoice.put(userId, serverId);
        }
        // user just left all voice channels
        else if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            System.out.println(userId + "Left Voice in guild" + serverId);
            inVoice.remove(userId);
        }
    }

    //Listener "listens" for start activity
    @Override
    public void onUserUpdateActivities(@NotNull UserUpdateActivitiesEvent event) {
        //Just to check if the method gets used at all
        System.out.println("onUserUpdateActivities Activated");

        if (event.getUser().isBot())
            return; //Ignore bots

        //Get userId & serverName
        String userId = event.getUser().getId();
        String serverId = event.getGuild().getId();
        String serverName = event.getGuild().getName();

        //Only track if they are currently in voice in that guild (server)
        if(!serverId.equals(inVoice.get(userId))) {
            return;
        }

        //Get activity on the user on the specific game they are playing, if it is null then the
        //user won't get any values since they do not exist, and if they do exist we get values and
        //then detect for game start and game stopped.
        //But if there are no activities from records,we assign an empty list so the loop doesn't crash
        //since the user is maybe starting a new game activity from start with no records.
        List<Activity> oldActivities = event.getOldValue() != null ? event.getOldValue() : List.of();
        List<Activity> newActivities = event.getNewValue() != null ? event.getNewValue() : List.of();


        //Start Game Activity detection
        for(Activity activity : newActivities) {
            if (activity.getType() == Activity.ActivityType.PLAYING &&
                    oldActivities.stream().noneMatch(a ->a.getName().equals(activity.getName()))) {
                System.out.println("Game detected for: " + userId + " started playing " + activity.getName());
                voiceSessionService.handleActivityStart(userId,serverId,serverName,activity.getName());
            }
        }

        //End Game Activity detection
        for(Activity activity : oldActivities) {
            if(activity.getType() == Activity.ActivityType.PLAYING &&
                    newActivities.stream().noneMatch(a -> a.getName().equals(activity.getName()))) {
                System.out.println("Game stopped for: " + userId + " stopped playing " + activity.getName());
                voiceSessionService.handleActivityEnd(userId,serverId,serverName, activity.getName());
            }
        }
    }
}
