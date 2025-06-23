package com.genetiicz.genbot.service;

import com.genetiicz.genbot.service.PlayTimeService;
import com.genetiicz.genbot.service.SlashService;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SlashService {

    private final PlayTimeService playTimeService;

    @Autowired
    public SlashService(PlayTimeService playTimeService) {
        this.playTimeService = playTimeService;
    }

    public void replyWithPlaytime(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String gameName = null;

        if (event.getOption("game") != null) {
            gameName = event.getOption("game").getAsString();
            System.out.println("Game option appear: " + gameName);
        } else {
            if (event.getMember() != null) {
                List<Activity> activities = event.getMember().getActivities();
                for (int i = 0; i < activities.size(); i++) {
                    Activity activity = activities.get(i);
                    if (activity.getType() == Activity.ActivityType.PLAYING) {
                        gameName = activity.getName();
                        System.out.println("Detected playing game: " + gameName);
                        break;
                    }
                }
            }
        }

        if (gameName == null) {
            System.out.println("The command is not working.");
            event.reply("You're not currently playing any game, or no game name was provided.").queue();
            return;
        }

        //Getting total minutes for how long we have played in our session
        //THis is a timebuilder
        Optional<Long> totalMinutesOpt = playTimeService.getTotalMinutesIncludingLive(userId, gameName);
        if (totalMinutesOpt.isPresent()) {
            long totalMinutes = totalMinutesOpt.get();
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;

            String message = "You have played **" + gameName + "** for a total of **";

            if (hours > 0) {
                message += hours + " hour" + (hours == 1 ? "" : "s");
            }
            if (hours > 0 && minutes > 0) {
                message += " and ";
            }
            if (minutes > 0 || hours == 0) {
                message += minutes + " minute" + (minutes == 1 ? "" : "s");
            }

            message += "**.";
            event.reply(message).queue();
        } else {
            event.reply("No playtime record found for: **" + gameName + "**.").queue();
        }
    }
    // Method and Logic to implement Top 5 players in a single game
    public void replyWithTop3 (SlashCommandInteractionEvent event) {
        String gameName = null;
    }
}
