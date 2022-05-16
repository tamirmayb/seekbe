package com.seekbe.parser.report;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor(staticName = "of")
public class ThreadPoolStats {

	private final int minThreads;
	private final int maxThreads;
	private final int activeThreads;
	private final int idleThreads;
	private final int largestPoolSize;

}
