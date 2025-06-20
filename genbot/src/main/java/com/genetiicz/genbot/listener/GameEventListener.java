package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.PlayTimeService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class GameEventListener extends ListenerAdapter {
    private final PlayTimeService playTimeService;

    //Automatically inject tables in PostgreSQL
    @Autowired
    public GameEventListener(PlayTimeService playTimeService) {
        this.playTimeService = playTimeService;
    }

    //To catch any events at all
    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        System.out.println("EVENT RECEIVED: " + event.getClass().getSimpleName());
    }



    //Controller "listens" for start activity
    @Override
    public void onUserUpdateActivities(@NotNull UserUpdateActivitiesEvent event) {
        System.out.println("onUserUpdatemethod Activated");
       if (event.getUser().isBot()) return; //Ignore bots

        //Get userId & serverName
        String userId = event.getUser().getId();
        String serverId = event.getGuild().getId();
        String serverName = event.getGuild().getName();

        //Get new activity (Detect game start)
        List<Activity> oldActivities = event.getOldValue();
        List<Activity> newActivities = event.getNewValue();


        //Check if the activity is a game so we track correctly
        for(Activity newActivity : newActivities) {
        if (newActivity.getType() == Activity.ActivityType.PLAYING && oldActivities.stream().noneMatch(a -> a.getName().equals(newActivity.getName())))
            //printing out system line for detecting the game for user in console
            System.out.println("Game Detected: User " + userId + "Started playing " + newActivity.getName());
        playTimeService.handleActivityStart(userId,serverId,serverName, newActivity.getName());
        }

        //Detect when the user is quitting game
        for(Activity oldActivity : oldActivities) {
           if (oldActivity.getType() == Activity.ActivityType.PLAYING && newActivities.stream().noneMatch(a -> a.getName().equals(oldActivity.getName())))
           //Print out system line so i can see if it is detecting
               System.out.println("Detected game stop: " + oldActivity.getName());
           playTimeService.handleActivityEnd(userId,serverId,serverName,oldActivity.getName());
        }
    }


}
