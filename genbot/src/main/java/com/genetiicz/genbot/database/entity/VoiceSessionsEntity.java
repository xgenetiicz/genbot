package com.genetiicz.genbot.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

//Lombok
@Getter
@Setter
//JPA annotations
@Entity
@Table(name = "voice_records")
public class VoiceSessionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)


    private Long id;

    @Column
    private String userId;

    @Column
    private String gameName;

    @Column
    private String serverId;

    @Column
    private String serverName;

    @Column
    private Instant joinTime;

    @Column
    private Instant leaveTime;

    @Column
    private boolean alertSent = false;
}
