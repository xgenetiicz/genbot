package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.PlayTimeService;
import com.genetiicz.genbot.service.SlashService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
public class SlashCommandListener extends ListenerAdapter {
    private final SlashService slashService;
    private final PlayTimeService playTimeService;

    //Constructor, by this can we access the PlaytimeData so we can retrieve
    // But we are not storing any data with slashcommands, just retrieving.
    @Autowired
    public SlashCommandListener(SlashService slashService, PlayTimeService playTimeService) {
        this.slashService = slashService;
        this.playTimeService = playTimeService;
    }

    @Override
    public void onSlashCommandInteraction (@NotNull SlashCommandInteractionEvent event) {
        //Need to check if it prints the actual command.
        System.out.println("Slash command " + event.getName());
        String command = event.getName();
        String userId = event.getUser().getId();

        //Making a case break since we are also going to implement a top 5 leaderboard.
        //First implementing the playtimeCommand
        switch (command) {
            case "playtime":
                //if statement to check the users activity/game info
                if (event.getMember() == null || event.getGuild() == null) {
                    event.reply("Unable to retrieve your current game.").queue();
                    return;
                }

                //Handling user typed gamename so they can retrieve their records
                String gameName;
                if(event.getOption("game")!= null) {
                    gameName = event.getOption("game").getAsString();
                } else {
                    List<Activity> activities = event.getMember().getActivities();
                    gameName = activities.stream().filter(activity -> activity.getType() == Activity.ActivityType.PLAYING)
                            .findFirst().map(Activity::getName).orElse(null);
                }

                if(gameName == null) {
                    event.reply("You're not currently playing any game, start the game to keep track! :D").queue();
                    return;
                }
                //Service that provide the output after using /playtime present
                playTimeService.getTotalMinutesIncludingLive(userId,gameName).ifPresentOrElse(totalMinutes -> {
                    long hours = totalMinutes / 60;
                    long minutes = totalMinutes % 60;
                    //Need to convert /playtime time visual in 00h and 00m format. Hours and minutes
                    StringBuilder timeBuilder = new StringBuilder();
                    if (hours > 0) {
                        timeBuilder.append(hours).append(" hour").append(hours == 1 ? "" : "s");
                    }
                    if (hours > 0 && minutes > 0){
                        timeBuilder.append(" and ");
                    }
                    if (minutes > 0 || hours > 0) {
                        timeBuilder.append(minutes).append(" minute").append(minutes == 1 ? "" : "s");
                    }
                    event.reply("You have played **" + gameName + "** for a total of **" + timeBuilder + "**.").queue();
                }, () -> event.reply("No playtime record found for: **" + gameName + "**.").queue());
                break;

                default:
                    event.reply("Unknown command, the correct command is: `/playtime`").queue();
        }
    }
}
