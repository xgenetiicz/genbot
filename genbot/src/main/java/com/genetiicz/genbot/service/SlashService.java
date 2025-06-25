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

    //Make a private helper method that convert all total minutes into
    //hours and minutes, I need this since I want to re-use it on several
    //methods, and need to access this within this class.
     //Creating the "timebuilder" String builder class again.
    private String formatPlayTime (long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        StringBuilder stringBuilder = new StringBuilder();
        if (hours > 0) {
            stringBuilder.append(hours).append(" hour").append(hours == 1 ? "" : "s");
        }
        if (hours > 0 && minutes > 0) {
            stringBuilder.append(" & ");
        }
        if (minutes > 0 || hours == 0) {
            stringBuilder.append(minutes).append(" minute").append(minutes == 1 ? "" : "s");
        }
        return stringBuilder.toString();
    }
    public void replyWithPlaytime(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String gameName = null;

        if (event.getOption("game") != null) {
            gameName = event.getOption("game").getAsString().trim();
            System.out.println("Game option appear: " + gameName);
        } else {
            //This check help with autocomplete because if not null we can retrieve arraylist
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
            String message = "You have played **" + gameName + "** for a total of **" + formatPlayTime(totalMinutes) + "**";
            event.reply(message).queue();
        } else {
            event.reply("No playtime record found for: **" + gameName + "**.").queue();
        }
    }
    // Method and Logic to implement Top 3 players in a single game
    public void replyWithPlaytimeTop3 (SlashCommandInteractionEvent event) {

        //Need the serverId so we can get guild, and guildmembers with their id.
        String serverId = event.getGuild().getId();

        if (event.getOption("game") == null) {
            event.reply("Please provide a game using the /playtimetop3 game:<Name of the game>").queue();
            return;
        }

        String gameName = event.getOption("game").getAsString().trim();
        List<PlayTimeEntity> topPlayers = playTimeService.get3TopPlayersForGame(gameName,serverId);

         //if the leaderboard list is empty, we want to display to the user that there are no players there.
        //after testing the bot, the check under is useless since if there are no records assigned to the server,
        //the output will show there are none records for the specific game, and if there is one user there, the leaderboard
        // will display -> so there is no point of keeping this check if the check never goes through. this check could only be valid
        // if I decide to implement later that the leaderboard needs to contain three people in order to show up, if not 3 users then an empty leaderboard.

        //if(topPlayers.isEmpty()) {
         //   event.reply("No users have played **" + correctedGameName + "** in this server").queue();
          //  return;
       // }

        //StringBuilder for leaderboard
        StringBuilder leaderboard = new StringBuilder();

        //Medals for the places 1,2,3.
        String[] medals = {"\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49"};

        for (int i = 0; i < topPlayers.size(); i++) {
            PlayTimeEntity entry = topPlayers.get(i);

            //Medals for the places 1,2,3.
            String medal = i < medals.length ? medals[i] : "`#`" + (i + 1);

            leaderboard.append(medal)
                    .append("**<@").append(entry.getUserId()).append(">") //Retrieve userId here with <@append.getUser>
                    //This is how Discord find userName
                    .append(": ").append(formatPlayTime(entry.getTotalMinutesPlayed()))
                    .append("**\n");
        }

        //EmbedBuilder for setting leaderboard
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("\uD83C\uDFC6 Top 3 Players for " + gameName);

        embedBuilder.setDescription(leaderboard.toString());
        //Vertical color change on the embedBuilder.
        embedBuilder.setColor(new Color(0, 0, 0));
        event.replyEmbeds(embedBuilder.build()).queue();
    }
}