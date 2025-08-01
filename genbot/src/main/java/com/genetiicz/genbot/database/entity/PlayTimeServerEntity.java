package com.genetiicz.genbot.database.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

//JPA annotations
@Entity
@Table(name = "playtime_server_records",uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "game_name", "server_id"}
    )
)
public class PlayTimeServerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(name = "user_id",       nullable = false) private String userId;
    @Column(name = "game_name",     nullable = false) private String gameName;
    @Column(name = "server_id",     nullable = false) private String serverId;
    @Column(name = "server_name",   nullable = false) private String serverName;

    //I did this because it's better to have global playtime so the user
    //can actually take their tracking to each server they are in.
    //I want to store the serverid and the servername they are on - for processing
    //data and analyze it easier - and also know what server user is on if something happens.

    //the leaderboard will be per-server tracking
    //the voiceSessionsEntity will alert the user of their playing time
    //in that PARTICULAR server. These Alerts will be deleted after end session.
}
