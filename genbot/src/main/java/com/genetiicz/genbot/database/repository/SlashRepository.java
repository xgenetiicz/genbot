package com.genetiicz.genbot.database.repository;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

//We make a new repository so we have seperation of concerns
//Ideally for having a clean architecture and understanding of project

@Repository
public interface SlashRepository extends JpaRepository<PlayTimeEntity,Long> {
    Optional<PlayTimeEntity> findByUserIdAndGameName (String userId,String gameName);
    List<PlayTimeEntity> findTop5ByGameNameAndServerIdOrderByTotalMinutesPlayedDesc(String gameName, String serverId);
}
