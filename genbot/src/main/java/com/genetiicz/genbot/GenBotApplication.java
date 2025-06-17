package com.genetiicz.genbot;

import com.genetiicz.genbot.controller.EventController; // Import your controller
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GenBotApplication {

	// Injects the bot token from application.properties
	@Value("${discord.bot.token}")
	private String botToken;

	public static void main(String[] args) {
		SpringApplication.run(GenBotApplication.class, args);
	}

	//Through bean will i create a JDA instance so the botToken can be used
	//And we call on our EventController where it communicates with our PlayTimeService.
	@Bean

	public JDA jda(EventController eventController) throws InterruptedException {
		JDABuilder builder = JDABuilder.createDefault(botToken);

		// Enable the necessary "Intent" to see user game activities
		builder.enableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MEMBERS);

		// Register the controller class to listen for Discord events
		builder.addEventListeners(eventController);

		// Build the JDA instance and wait for connection.
		return builder.build().awaitReady();
	}
}