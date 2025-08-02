package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Comparator;
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
        String serverId = event.getGuild().getId();

        String gameName = null;
        if (event.getOption("game") != null) {
            gameName = event.getOption("game").getAsString().trim();
            System.out.println("Game option appear: " + gameName);
        }

        if (gameName == null) {
            System.out.println("The command is not working.");
            event.reply("Please select a game using the autocompletion list provided.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // Make sure that the autocompletion is actually checking the list retrieved
        // Since the user can mistype and i need to enforce the strict of the autocompletion.
        List<PlayTimeEntity> validGames = playTimeService.getGamesPlayedByUser(userId, serverId);

        if (validGames.isEmpty()) {
            event.reply("You haven't played any tracked games in this **server** yet.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // checking if the user has played by boolean with using validGames,
        // so we can format it later with totalMinutesOpt.
        String finalGameName = gameName;
        boolean hasPlayed = validGames.stream()
                .anyMatch(entry -> entry.getGameName().equalsIgnoreCase(finalGameName));

        // if they haven't played, suggest a best‐match or bail out
        if (!hasPlayed) {
            // Also adding levenshtein matching for user that writes wrong
            LevenshteinDistance levenshtein = new LevenshteinDistance();
            String bestMatch = validGames.stream()
                    .map(PlayTimeEntity::getGameName)
                    .min(Comparator.comparingInt(name ->
                            levenshtein.apply(name.toLowerCase(), finalGameName.toLowerCase())))
                    .orElse(null);

            if (bestMatch != null) {
                event.reply("You haven't played **" + gameName + "** in this server.\n" +
                                "Did you mean **" + bestMatch + "**?")
                        .setEphemeral(true)
                        .queue();
            } else {
                event.reply("You haven't played **" + gameName + "** in this server. " +
                                "Please select a game from the list.")
                        .setEphemeral(true)
                        .queue();
            }
            return;
        }

        // Getting total minutes for how long we have played in our session
        Optional<Long> totalMinutesOpt =
                playTimeService.getTotalMinutesIncludingLive(userId, gameName);

        if (totalMinutesOpt.isPresent()) {
            long totalMinutes = totalMinutesOpt.get();
            event.reply("You have played **" + gameName +
                            "** for a total of **" + formatPlayTime(totalMinutes) + "**")
                    .queue();
        } else {
            event.reply("No game or playtime record found for **" + gameName + "**.")
                    .setEphemeral(true)
                    .queue();
        }
    }


    // Method and Logic to implement Top 3 players in a single game
    public void replyWithPlaytimeTop3 (SlashCommandInteractionEvent event) {

        //Need the serverId so we can get guild, and guildmembers with their id.
        String serverId = event.getGuild().getId();

        if (event.getOption("game") == null) {
            event.reply("Please select a game from the autocompletion list.").setEphemeral(true).queue();
            return;
        }

        String gameName = event.getOption("game").getAsString().trim();
        List<PlayTimeEntity> topPlayers = playTimeService.get3TopPlayersForGame(gameName,serverId);

         //if the leaderboard list is empty, we want to display to the user that there are no players there.
        //after testing the bot, the check under is useless since if there are no records assigned to the server,
        //the output will show there are none records for the specific game, and if there is one user there, the leaderboard
        // will display -> so there is no point of keeping this check if the check never goes through. this check could only be valid
        // if I decide to implement later that the leaderboard needs to contain three people in order to show up, if not 3 users then an empty leaderboard.
        if(topPlayers.isEmpty()) {

                //get distinct games for leaderboard also -- levenshtein distance
                List<String> allGames = playTimeService.getMatchingGamesStartingWith("" ,serverId,Integer.MAX_VALUE);
                LevenshteinDistance lev = new LevenshteinDistance();

                //Closest match to all games
            String bestMatch = allGames.stream()
                    .min(Comparator.comparingInt(g ->
                            lev.apply(g.toLowerCase(), gameName.toLowerCase())))
                    .orElse(null);

            String reply;
            if(bestMatch != null && !bestMatch.equalsIgnoreCase(gameName)) {
                reply = "No one has played **" + gameName + "** on this server yet.\n"
                        + "Did you mean **" + bestMatch + "**?";
            } else {
                reply = "There are **no records** of this game by any user in this **server**";
            }
            event.reply(reply).setEphemeral(true).queue();
                return;
        }

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

    //method for finding friends time on the same server
    public void replyWithFriendTime(SlashCommandInteractionEvent event) {
        OptionMapping friendOpt = event.getOption("friend");
        if(friendOpt == null) {
            event.reply("You must specify a friend to check!").setEphemeral(true).queue();
            return;
        }
        String friendId = friendOpt.getAsUser().getId();
        String serverId = event.getGuild().getId();
        String gameName = event.getOption("game").getAsString();

        Optional<Long> friendTime = playTimeService.getFriendTotalMinutes(friendId,gameName,serverId);
        if(friendTime.isEmpty() || friendTime.get() == 0L){
            event.reply("No record playtime for friend: <@" + friendId + "> on **" + gameName + "**.")
                    .setEphemeral(true).queue();
            return;
        }
        long total = friendTime.get();
        long hours = total / 60;
        long minutes = total % 60;

        String message = String.format("<@%s> has played **%s** for %d hour%s and %d minute%s.",
                friendId, gameName,hours, (hours == 1 ? "" : "s"),
                minutes, (minutes == 1 ? "" : "s")
                );
        //reply this message with an event so we don't spam the chat
        event.reply(message).setEphemeral(true).queue();

    }
}