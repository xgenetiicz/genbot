package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
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
    public void replyWithPlaytimeTop3 (SlashCommandInteractionEvent event) {
        String gameName = null;
        if (event.getOption("game") != null) {
            //getting the gameName they are playing as an String
            gameName = event.getOption("game").getAsString().trim(); //trim for no spaces

        }
        if(gameName == null || gameName.isBlank()) {
            event.reply("Please provide a game name using `/playtimeTop3 -> Game:<Game>`").queue();
            return;
        }
        if (event.getGuild() == null) {
            event.reply("This command must be used in a server").queue();
            return;
        }
        //Need the serverId so we can get guild, and guildmembers with their id.
        String serverId = event.getGuild().getId();
        List<PlayTimeEntity> topPlayers = playTimeService.get3TopPlayersForGame(gameName,serverId);
        //If statement to check if the guild has any playtime data on that game.
        if(topPlayers.isEmpty()) {
            event.reply("No playtime record found for **" + gameName + "** in this server.").queue();
            return;
        }
        //StringBuilder for leaderboard
        StringBuilder leaderboard = new StringBuilder();
        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};
        for (int i = 0; i < topPlayers.size(); i++){
            PlayTimeEntity entry = topPlayers.get(i);
            //Medals for the places 1,2,3.
            String medal = i < medals.length ? medals [i] : "`#`" + (i + 1);
            leaderboard.append(medal).append("<@").append(entry.getUserId()).append(">")
                    .append(": ").append(entry.getTotalMinutesPlayed()).append(" minutes\n");
        }
        //EmbedBuilder for setting leaderboard
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("\uD83C\uDFC6 Top 3 Players for " + gameName);

        embedBuilder.setDescription(leaderboard.toString());
        //Vertical color change on the embedBuilder.
        embedBuilder.setColor(new Color(0,0,0));

        event.replyEmbeds(embedBuilder.build()).queue();
    }
}