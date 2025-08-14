package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.VoiceSessionsEntity;
import com.genetiicz.genbot.database.repository.VoiceSessionsRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@EnableScheduling
public class VoiceSessionService {
    private final VoiceSessionsRepository voiceSessionsRepository;
    private final PlayTimeService playTimeService;


    //Value for which text channel it should go to:
    @Value("${voice.alert.channel-id}")
    private String channelId;

    //these values are for the config "application.properties"
    @Value("${voice.alert.threshold-minutes}")
    private int thresholdMinutes;
    @Value("${voice.alert.window-start}")
    private String windowStartStr;
    @Value("${voice.alert.window-end}")
    private String windowEndStr;

    //Inject Repository
    @Autowired
    public VoiceSessionService(VoiceSessionsRepository voiceSessionsRepository,PlayTimeService playTimeService) {
        this.voiceSessionsRepository = voiceSessionsRepository;
        this.playTimeService = playTimeService;

    }

    //Splitting the injection for the JDA for voice
    @Autowired @Lazy
    private JDA jda;

    public void handleActivityStart(String userId, String serverId, String serverName, String gameName) {
        //before we process - need to check if the user has a active voice session, and if not we will process data to voice_records
        boolean alreadyOpen = voiceSessionsRepository.
                findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc(userId, serverId, serverName, gameName).
                isPresent();
        if(alreadyOpen) {
            return;
        }

        //process data to voice_records
        VoiceSessionsEntity voiceRecord = new VoiceSessionsEntity();
        voiceRecord.setUserId(userId);
        voiceRecord.setServerId(serverId);
        voiceRecord.setServerName(serverName);
        voiceRecord.setGameName(gameName);
        voiceRecord.setJoinTime(Instant.now());
        voiceRecord.setAlertSent(false);

        //save the record
        voiceSessionsRepository.save(voiceRecord);
    }

    //Stop the listener, and we will use endSession here so we save it instant
    public void handleActivityEnd(String userId, String serverId, String serverName, String gameName) {
        voiceSessionsRepository.findFirstByUserIdAndServerIdAndServerNameAndGameNameIgnoreCaseAndLeaveTimeIsNullOrderByJoinTimeDesc
                (userId, serverId, serverName, gameName).ifPresent(session ->  {
            session.setLeaveTime(Instant.now());
            voiceSessionsRepository.save(session);
        });
    }

    //Alert method with scheduling so it tracks and give user response.
    @Scheduled(fixedRate = 60_000)
    public void checkVoiceAlert() {
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(windowStartStr, DateTimeFormatter.ISO_TIME);
        LocalTime end   = LocalTime.parse(windowEndStr,   DateTimeFormatter.ISO_TIME);
        boolean inWindow = start.isBefore(end)
                ? !now.isBefore(start) && now.isBefore(end)
                : !now.isBefore(start) || now.isBefore(end);
        //if statement to check if the method can't find the voice channel
        TextChannel channel = jda.getTextChannelById(channelId);

        if(channel == null) {
            System.err.println("VoiceSessionService: cannot find channel: **" + channelId + "**. ");
            return;
        } else {
            System.out.println("VoiceSessionService: Found the channel: **" + channelId + "**. ");
        }

        List<VoiceSessionsEntity> open =
                voiceSessionsRepository.findByLeaveTimeIsNullAndAlertSentFalse();

        for (VoiceSessionsEntity sessions : open) {
            long mins = Duration.between(sessions.getJoinTime(), Instant.now()).toMinutes();
            if (mins >= thresholdMinutes) {
                String laugh = "\uD83D\uDE02";
                String message = String.format(
                        "**Bro**  pust litt bruttern hehehehe bare spiller hele dagen.. ta deg en luftetur eller noe" + laugh + " %s\nDu er jo helt spinnvill <@%s> på **%s**!",
                        sessions.getUserId(), sessions.getGameName()
                );
                if (inWindow) {
                    channel.sendMessage(message).queue();
                } else {
                    channel.sendMessage("Outside window: " + message).queue();
                }

                //save the session and alert.
                sessions.setAlertSent(true);
                voiceSessionsRepository.save(sessions);
            }
        }
    }

    @Transactional
    public void endAllOpenSessionsForUser(String userId, String serverId) {
        List<VoiceSessionsEntity> open = voiceSessionsRepository.findByUserIdAndServerIdAndLeaveTimeIsNull(userId, serverId);

        //Make this instant so we guarantee that leave - time gets set even if gameName is or was null
        Instant now = Instant.now();
        for (VoiceSessionsEntity session : open) {
            session.setLeaveTime(now);
        }
        voiceSessionsRepository.saveAll(open);
    }
}
