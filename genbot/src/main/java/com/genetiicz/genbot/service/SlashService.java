package com.genetiicz.genbot.service;

import com.genetiicz.genbot.database.entity.PlayTimeEntity;
import com.genetiicz.genbot.database.repository.SlashRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SlashService {
    private final SlashRepository slashRepository;

    @Autowired
    public SlashService(SlashRepository slashRepository) {
        this.slashRepository = slashRepository;
    }

    public Optional<PlayTimeEntity> findByUserIdAndGameName (String userId, String gameName) {
        return slashRepository.findByUserIdAndGameName(userId,gameName);
    }

    //public List<PlayTimeEntity>findTop5ByGameNameAndServerIdOrderByTotalMinutesPlayedDesc(String serverId, String gameName) {
     //   return slashRepository.findTop5ByGameNameAndServerIdOrderByTotalMinutesPlayedDesc(serverId, gameName);
   // }

}