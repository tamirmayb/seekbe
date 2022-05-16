package com.seekbe.parser.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class SchedulerStats {

	private final long jobsCount;
	private final ThreadPoolStats threadPoolStats;

}
