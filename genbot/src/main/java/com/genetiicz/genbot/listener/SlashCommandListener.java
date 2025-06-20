package com.genetiicz.genbot.listener;

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

    //Constructor, by this can we access the PlaytimeData so we can retrieve
    // But we are not storing any data with slashcommands, just retrieving.
    @Autowired
    public SlashCommandListener(SlashService slashService) {
        this.slashService = slashService;
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
                slashService.findTopByUserIdAndGameNameIgnoreCaseOrderByUpdatedAtDesc(userId,gameName).ifPresentOrElse(
                        PlayTimeEntity -> event.reply("You have played " + gameName + " in total for " + PlayTimeEntity.getTotalMinutesPlayed() + " minutes.").queue(),
                        () -> event.reply("No playtime record found for: " + gameName +".").queue()
                );
                break;

            default:
                event.reply("Unknown command, the correct command is: /playtime").queue();
        }
    }
}
