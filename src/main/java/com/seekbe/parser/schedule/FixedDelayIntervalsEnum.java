package com.seekbe.parser.schedule;

import lombok.Getter;
import lombok.ToString;

import java.time.Duration;

@Getter
@ToString
public enum FixedDelayIntervalsEnum {
    ONE_HOUR("Every 1 hour", Duration.ofHours(1)),
    TWO_HOURS("Every 2 hours", Duration.ofHours(2)),
    SIX_HOURS("Every 6 hours", Duration.ofHours(6)),
    TWELVE_HOURS("Every 12 hours", Duration.ofHours(12));

    FixedDelayIntervalsEnum(String desc, Duration duration) {
        this.desc = desc;
        this.duration = duration;
    }
    String desc;
    Duration duration;
}
