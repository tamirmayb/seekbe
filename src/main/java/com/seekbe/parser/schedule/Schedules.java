package com.seekbe.parser.schedule;

import java.time.Duration;

public class Schedules {

	public static Schedule fixedDelaySchedule(FixedDelayIntervalsEnum fixedDelayIntervalsEnum) {
		return new FixedDelaySchedule(fixedDelayIntervalsEnum.getDuration());
	}

	public static Schedule fixedDelaySchedule(Duration duration) {
		return new FixedDelaySchedule(duration);
	}

	public static Schedule executeOnce(Schedule schedule) {
		return new OneTimeSchedule(schedule);
	}

	public static Schedule executeOnce() {
		return new OneTimeSchedule(fixedDelaySchedule(Duration.ZERO));
	}

	public static Schedule afterInitialDelay(Schedule schedule, Duration initialDelay) {
		return new AfterInitialDelaySchedule(schedule, initialDelay);
	}

}
