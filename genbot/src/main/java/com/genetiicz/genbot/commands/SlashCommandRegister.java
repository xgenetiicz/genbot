package com.genetiicz.genbot.commands;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

@Component
public class SlashCommandRegister {
    private final JDA jda;

    public SlashCommandRegister(JDA jda) {
        this.jda = jda;
        //Need to check if Spring is using the class at all.
        System.out.println("SlashCommandRegister class is being used");
    }

    @PostConstruct
    public void registerSlashCommand () {
        jda.updateCommands().addCommands(
                Commands.slash("myplaytime", "See how long you have been playing the game in **total**.")
                        .addOption(OptionType.STRING, "game", "Name of the game", true,true),

                 Commands.slash("leaderboard", "See top 3 most active players in this **game**.")
                         .addOption(OptionType.STRING, "game", "Name of the game", true,true), //we want autocomplete on this one.

                Commands.slash("friendplaytime", "See your friend's global playtime! NB: **Both needs to be on the same server**")
                        .addOption(OptionType.USER, "friend", "Select or type the user", true)
                        .addOptions(gameOption()),

                Commands.slash("info", "See a quick overview of all PlayTimeBot commands.")
        ).queue();

        System.out.println("Slash commands 'myplaytime','leaderboard', 'friendplaytime' and 'info' is registered!");

        // to retrieve commands to see if new ones are registered. iterates the list of registered commands
        // where this confirm the command is there - Since Discord can use time to update the command through API

        jda.retrieveCommands().queue(cmds -> {
            System.out.println("Global commands currently registered:");
            cmds.forEach(c -> System.out.println("  — " + c.getName()));
        });

    }

    private OptionData gameOption() {
        return new OptionData(OptionType.STRING, "game", "name of the game").setRequired(true).setAutoComplete(true);
    }
}
