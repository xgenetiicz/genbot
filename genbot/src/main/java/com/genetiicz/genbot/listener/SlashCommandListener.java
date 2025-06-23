package com.genetiicz.genbot.listener;

import com.genetiicz.genbot.service.SlashService;
import jakarta.annotation.PostConstruct;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class SlashCommandListener extends ListenerAdapter {
    private final SlashService slashService;
    //We are making changes and making a map for slashcommands so we can add easily more commands into the register.
    //By this map we iterate and the listener listens to which commmand get used.
    private final Map<String, Consumer<SlashCommandInteractionEvent>> handlers = new HashMap<>();

    @Autowired
    public SlashCommandListener(SlashService slashService) {
        this.slashService = slashService;
    }

    @PostConstruct
    public void initHandlers() {
        handlers.put("playtime", slashService::replyWithPlaytime);
        //here we can add more handlers.put for several commands for the bot.
        //Since this worked, now we implement Top5 command inside the handler
       // handlers.put("playtimeTop5", slashService::replyWithPlaytimeTop5);
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
}