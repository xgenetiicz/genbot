package com.genetiicz.genbot.commands;

import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
                Commands.slash("playtime", "See how long you have been playing the game.")
           //   Commands.slash("playtimetop5", "See top 5 most active players in this game.")
        ).queue();

        System.out.println("Slash commands 'playtime' is registered!");
    }
}
