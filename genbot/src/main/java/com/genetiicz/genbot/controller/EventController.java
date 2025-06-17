package com.genetiicz.genbot.controller;

import com.genetiicz.genbot.service.PlayTimeService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Controller
public class EventController extends ListenerAdapter {
    private final PlayTimeService playTimeService;

    //Automatically inject tables in PostgreSQL
    @Autowired
    public EventController(PlayTimeService playTimeService) {
        this.playTimeService = playTimeService;
    }
    //Controller "listens" for start activity
    @Override
    public void onUserActivityStart(UserActivityStartEvent event) {
        //Debugging, the bot isn't detecting the activity.
       System.out.println("Activity start for user: " + event.getUser().getId());

        //Get userId & serverName
        String userId = event.getUser().getId();
        String serverId = event.getGuild().getId();
        String serverName = event.getGuild().getName();

        //Get new activity (new game or same game)
        Activity newActivity = event.getNewActivity();


        //Check if the activity is a game so we track correctly
        if (newActivity != null && newActivity.getType() == Activity.ActivityType.PLAYING) {
            String gameName = newActivity.getName();
            System.out.println("Game Detected: User " + userId + "Started playing " + gameName);

            //Connect it to playTimeService where this is our "listener"
            playTimeService.handleActivityStart(userId,serverId,serverName,gameName);
        }
    }
    //Controller "listens" for end activity
    @Override
    public void onUserActivityEnd (UserActivityEndEvent event) {
        //userId & serverName
        String userId = event.getUser().getId();
        String serverId = event.getGuild().getId();
        String serverName = event.getGuild().getName();

        //End game activity (new game or same game)
        Activity oldActivity = event.getOldActivity();

        //If statement to check if the user was actually playing a game
        if (oldActivity != null && oldActivity.getType() == Activity.ActivityType.PLAYING) {
            String gameName = oldActivity.getName();
            playTimeService.handleActivityEnd(userId,serverId, serverName, gameName);
        }
    }
}
