package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.VoiceSessionsEntity;
import com.genetiicz.genbot.database.repository.VoiceSessionsRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class VoiceSessionService {
    private final VoiceSessionsRepository voiceSessionsRepository;
    private final PlayTimeService playTimeService;
    private final VoiceAlertProperties props;

    @Autowired
    public VoiceSessionService(VoiceSessionsRepository voiceSessionsRepository, PlayTimeService playTimeService,VoiceAlertProperties props) {
        this.voiceSessionsRepository = voiceSessionsRepository;
        this.playTimeService = playTimeService;
        this.props = props;
    }

    // Lazy injection so JDA is available after Spring bootstraps everything
    @Autowired @Lazy
    private JDA jda;

    public void handleActivityStart(String userId, String serverId, String serverName, String gameName) {
        boolean alreadyOpen = voiceSessionsRepository
                .findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc(
                        userId, serverId, serverName, gameName
                ).isPresent();
        if (alreadyOpen) {
            return;
        }

        // process data to voice_records
        VoiceSessionsEntity voiceRecord = new VoiceSessionsEntity();
        voiceRecord.setUserId(userId);
        voiceRecord.setServerId(serverId);
        voiceRecord.setServerName(serverName);
        voiceRecord.setGameName(gameName);
        voiceRecord.setJoinTime(Instant.now());
        voiceRecord.setAlertSent(false);
        voiceSessionsRepository.save(voiceRecord);
    }

    //called when user leaves voice so we stop the tracking
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName) {
        voiceSessionsRepository
                .findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc(
                        userId, serverId, serverName, gameName
                ).ifPresent(session -> {
                    session.setLeaveTime(Instant.now());
                    voiceSessionsRepository.save(session);
                });
    }

    //runs every minute and check all alerts that are false
    // if hit threshold - the alert becomes true
    // Alert method with scheduling so it tracks and give user response.
    @Scheduled(fixedRate = 60_000)
    public void checkVoiceAlert() {

        // get current time and window bounds
        LocalTime now   = LocalTime.now();
        LocalTime start = LocalTime.parse(props.getWindowStart(), DateTimeFormatter.ISO_TIME);
        LocalTime end   = LocalTime.parse(props.getWindowEnd(),   DateTimeFormatter.ISO_TIME);

        // determine if we’re inside the allowed alert window
        boolean inWindow = start.isBefore(end)
                ? !now.isBefore(start) && now.isBefore(end)
                : !now.isBefore(start) || now.isBefore(end);

        // fetch all open sessions that haven’t yet been alerted
        List<VoiceSessionsEntity> open =
                voiceSessionsRepository.findByLeaveTimeIsNullAndAlertSentFalse();

        for (VoiceSessionsEntity session : open) {
            // compute how many minutes this user has been in voice
            long mins = Duration.between(session.getJoinTime(), Instant.now()).toMinutes();
            // only alert once they’ve passed the threshold
            if (mins < props.getThresholdMinutes()) {
                continue;
            }

            // lookup the one channel mapped for this server
            String serverId  = session.getServerId();
            String channelId = props.getChannelMap().get(serverId);;
            if (channelId == null) {
                // no mapping defined for this guild → skip
                System.err.println("VoiceSessionService: no channel mapping for server: " + serverId);
                continue;
            }

            // attempt to resolve the TextChannel
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel == null) {
                // channel invalid or bot can’t see it
                System.err.println("VoiceSessionService: cannot find channel: **" + channelId + "**.");
                continue;
            } else {
                System.out.println("VoiceSessionService: Found the channel: **" + channelId + "**.");
            }

            // compose your meme-tastic alert
            String laugh = "\uD83D\uDE02";
            String message = String.format(
                    "**Bakzuz** ta så slapp av litt, pust litt bruttern hehehehe bare spiller hele dagen.. %s\n" +
                            "Du er jo helt spinnvill <@%s> på **%s**!",
                    laugh,
                    session.getUserId(),
                    session.getGameName()
            );

            // only send if we’re in the configured time window
            if (inWindow) {
                channel.sendMessage(message).queue();
            } else {
                channel.sendMessage("Outside window: " + message).queue();
            }

            // mark this session as alerted so we don’t spam again
            session.setAlertSent(true);
            voiceSessionsRepository.save(session);

        }
    }

    @PostConstruct
    public void dumpMappings() {
        System.out.println("Loaded voice.alert.channel-map: " + props.getChannelMap());
    }



    //marks the open sessions so when the user leaves it closes immediately.
    @Transactional
    public void endAllOpenSessionsForUser(String userId, String serverId) {
        List<VoiceSessionsEntity> open =
                voiceSessionsRepository.findByUserIdAndServerIdAndLeaveTimeIsNull(userId, serverId);
        Instant now = Instant.now();

        for (VoiceSessionsEntity ses : open) {
            ses.setLeaveTime(now);
        }
        voiceSessionsRepository.saveAll(open);
    }
}
