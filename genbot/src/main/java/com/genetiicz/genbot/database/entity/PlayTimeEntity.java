package com.genetiicz.genbot.database.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

//Lombok annotations
@Getter
@Setter
@NoArgsConstructor
//JPA annotations
@Entity
@Table(name = "playtime_records") //the name of the table in PostgreSQL
public class PlayTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //Auto - incremeting primary key
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String serverId;

    @Column(nullable = false)
    private String serverName;

    @Column(nullable = false)
    private String gameName;

    @Column(nullable = false)
    private long totalMinutesPlayed;

    //Timestamp for when the record was last updated
    @Column
    @LastModifiedDate
    private java.time.Instant updatedAt;
}
