package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.VoiceSessionService;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceEventListener extends ListenerAdapter {
    private final VoiceSessionService voiceSessionService;
    //the idea is only to track now when they are in voice in the particular server // moved this to here since we will store
    // in a new entity
    @Getter
    private final Map<String,String> inVoice = new ConcurrentHashMap<>();

    @Autowired
    public VoiceEventListener(VoiceSessionService voiceSessionService){
        this.voiceSessionService =  voiceSessionService;
    }

    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot()) return; //check bot

        String userId = event.getMember().getId();
        String serverId = event.getGuild().getId();
        String serverName = event.getGuild().getName();

        // what game they are actually playing.
        String gameName = event.getMember().getActivities().stream().filter(activity -> activity.getType()
        == Activity.ActivityType.PLAYING).map(Activity::getName).findFirst().orElse(null);

        //Join event:
        if (event.getChannelJoined() != null) {
            inVoice.put(userId,serverId);
            System.out.println(userId + " Joined voice in guild " + serverId);
            if(gameName != null) {
                voiceSessionService.handleActivityStart(userId,serverId,serverName,gameName);
            }
        }

        // Leave event:
        else if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
            System.out.println(userId + "Left Voice in guild" + serverId);
            inVoice.remove(userId);
            voiceSessionService.endAllOpenSessionsForUser(userId,serverId);
        }
    }

    public boolean isUserInVoice(String userId, String serverId) {
        return serverId.equals(inVoice.get(userId));
    }
}
