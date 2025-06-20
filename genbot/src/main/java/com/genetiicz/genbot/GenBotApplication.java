package com.genetiicz.genbot;

import com.genetiicz.genbot.listener.GameEventListener;
import com.genetiicz.genbot.listener.SlashCommandListener;
import com.genetiicz.genbot.service.PlayTimeService;
import com.genetiicz.genbot.service.SlashService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing //
@SpringBootApplication
public class GenBotApplication {

	// Injects the bot token from application.properties
	@Value("${discord.bot.token}")
	private String botToken;

	public static void main(String[] args) {
		SpringApplication.run(GenBotApplication.class, args);
	}

	//Through bean will i create a JDA instance so the botToken can be used
	//And we call on our gameEventListener where it communicates with our PlayTimeService.
	@Bean
	public GameEventListener gameEventListener(PlayTimeService playTimeService){
		return new GameEventListener(playTimeService);
	}

	@Bean
	public SlashCommandListener slashCommandListener (SlashService slashService) {
		return new SlashCommandListener(slashService);
	}

	@Bean
	public JDA jda(GameEventListener gameEventListener, SlashCommandListener slashCommandListener) throws InterruptedException {
		JDABuilder builder = JDABuilder.createDefault(botToken);
		System.out.println("Token is configured and running!");

		// Enable the necessary "Intent" to see user game activities
		builder.enableIntents(
				GatewayIntent.GUILD_PRESENCES,
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.MESSAGE_CONTENT
		);
		System.out.println("Gateways are intact and working as intended, in code and in dev portal(Check Again)!");

		//Setting member Cache policy since the Controller is listening to all generic events but not game.
		builder.setMemberCachePolicy(MemberCachePolicy.ALL);
		builder.setChunkingFilter(ChunkingFilter.ALL);
		builder.enableCache(CacheFlag.ACTIVITY);

		// Register the controller class to listen for Discord events
		builder.addEventListeners(gameEventListener);
		builder.addEventListeners(slashCommandListener);

		//if statement to see if they actually build correctly.
		 if(gameEventListener != null) {
			System.out.println("gameEventListener is working");
		 } else {
			 System.out.println("gameEventListener is not working and failed to build.");
		 }

		 if(slashCommandListener != null) {
			 System.out.println("slashCommandListener is working as intended.");
		 } else {
			 System.out.println("slashCommandListener is not working as intended, failed to build");
		 }

		// Build the JDA instance and wait for connection.
			//need to debug with try -catch
		try {
			JDA jda = builder.build().awaitReady();
			System.out.println("JDA is ready and connected");
				//Load all members for tracking presence.
				 jda.getGuilds().forEach(guild ->
						guild.loadMembers().onSuccess(members ->
								System.out.println("Loaded " + members.size() + "members for guild" + guild.getName())));
			return jda;
		} catch (InterruptedException e) {
			System.err.println("JDA is not working as intended");
			Thread.currentThread().interrupt(); // try to restore interrupted state
			return null;
		} catch (Exception e) {
			System.err.println("Failed to build JDA" + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}