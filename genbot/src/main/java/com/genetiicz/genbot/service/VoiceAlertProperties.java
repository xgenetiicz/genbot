package com.genetiicz.genbot.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties(prefix = "voice.alert")
public class VoiceAlertProperties {
    /**
     * Maps guild-ID → text-channel-ID
     */
    private Map<String,String> channelMap = new HashMap<>();

    /** How many minutes until we trigger the alert */
    private int thresholdMinutes;

    /** Daily window start (ISO local-time) */
    private String windowStart;

    /** Daily window end (ISO local-time) */
    private String windowEnd;

    // ─── getters & setters ─────────────────────────────────────────────────────

    public Map<String, String> getChannelMap() {
        return channelMap;
    }
    public void setChannelMap(Map<String, String> channelMap) {
        this.channelMap = channelMap;
    }

    public int getThresholdMinutes() {
        return thresholdMinutes;
    }
    public void setThresholdMinutes(int thresholdMinutes) {
        this.thresholdMinutes = thresholdMinutes;
    }

    public String getWindowStart() {
        return windowStart;
    }
    public void setWindowStart(String windowStart) {
        this.windowStart = windowStart;
    }

    public String getWindowEnd() {
        return windowEnd;
    }
    public void setWindowEnd(String windowEnd) {
        this.windowEnd = windowEnd;
    }
}
