package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.PlayTimeService;
import com.genetiicz.genbot.service.SlashService;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class SlashCommandListener extends ListenerAdapter {
    private final SlashService slashService;
    private final PlayTimeService playTimeService;
    //We are making changes and making a map for slashcommands so we can add easily more commands into the register.
    //By this map we iterate and the listener listens to which commmand get used.
    private final Map<String, Consumer<SlashCommandInteractionEvent>> handlers = new HashMap<>();

    @Autowired
    public SlashCommandListener(SlashService slashService,PlayTimeService playTimeService) {
        this.slashService = slashService;
        this.playTimeService = playTimeService;
    }

    @PostConstruct
    public void initHandlers() {
        //handler with the methods of replyWithPlaytime
        //it is supposed to tell the user if the user does not have any games on the server recorded.
        handlers.put("myplaytime", slashService::replyWithPlaytime);
        //here we can add more handlers.put for several commands for the bot.
        //Since this worked, now we implement Top5 command inside the handler
        handlers.put("leaderboard", slashService::replyWithPlaytimeTop3);
    }

    @Override
    public void onSlashCommandInteraction (@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        if(handlers.containsKey(commandName)) {
            handlers.get(commandName).accept(event);
        } else {
            event.reply("Unknown command.").queue();
        }
    }
    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        String commandName = event.getName();
        String focusedOption = event.getFocusedOption().getName();

        if (!focusedOption.equals("game")) {
            return;
        }

        //We need to get values from input and get the server the user is on
        String input = event.getFocusedOption().getValue();
        String serverId = event.getGuild().getId();
        String userId = event.getUser().getId();


        //List for fetching matching games from service max 10 choices.
        //We are also adding userId here, so we can fetch the auto-completion for /myplaytime
        //and the idea is to only retrieve the list of games the user has played.
        List<String> gameSuggestions = playTimeService.getMatchingGamesStartingWith(input, serverId, 10); // we want do display max 10 choices, discord has 25 general choices.
            if(commandName.equals("myplaytime")) {
                gameSuggestions = playTimeService.getMatchingGamesForUserStartingWith(input,serverId,userId,10);
            } else {
                gameSuggestions = playTimeService.getMatchingGamesStartingWith(input,serverId,10);
            }
        //Build autocomplete choices and display in a array list
        List<Command.Choice> choices = new ArrayList<>();
        for(String name : gameSuggestions) {
            Command.Choice choice = new Command.Choice(name, name);
            choices.add(choice);
        }
        //Reply with choices
        if(commandName.equals("leaderboard") || commandName.equals("myplaytime")){
            event.replyChoices(choices).queue();
        }
    }
}