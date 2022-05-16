package com.seekbe.parser;

import com.seekbe.parser.report.SchedulerStats;
import com.seekbe.parser.report.ThreadPoolStats;
import com.seekbe.parser.schedule.Schedule;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An instance reference a group of jobs
 * Responsible for scheduling jobs
 * A job is executed only once at a time.
 * The scheduler will never execute the same job twice at a time.
 **/
public final class Scheduler {

	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	private static final AtomicInteger threadCounter = new AtomicInteger(0);

	private final ThreadPoolExecutor threadPoolExecutor;
	private final AtomicBoolean launcherNotifier;

	// jobs
	private final Map<String, Job> indexedJobsByName;
	private final ArrayList<Job> nextExecutionsOrder;
	private final Map<String, CompletableFuture<Job>> cancelHandles;

	private volatile boolean shuttingDown;

	// constructors

	/**
	 * Create a scheduler with defaults
	 */
	public Scheduler() {
		this(SchedulerConfig.builder().build());
	}

	public Scheduler(int maxThreads) {
		this(SchedulerConfig.builder().maxThreads(maxThreads).build());
	}

	public Scheduler(SchedulerConfig config) {
		this.indexedJobsByName = new ConcurrentHashMap<>();
		this.nextExecutionsOrder = new ArrayList<>();
		this.launcherNotifier = new AtomicBoolean(true);
		this.cancelHandles = new ConcurrentHashMap<>();
		this.threadPoolExecutor = new com.seekbe.parser.ScalingThreadPoolExecutor(
			config.getMinThreads(),
			config.getMaxThreads(),
			config.getThreadsKeepAliveTime().toMillis(),
			TimeUnit.MILLISECONDS,
			new ThreadFactory()
		);
		// run job launcher thread
		Thread launcherThread = new Thread(this::launcher, "Scheduler Monitor");
		if (launcherThread.isDaemon()) {
			launcherThread.setDaemon(false);
		}
		launcherThread.start();
	}

	public Job schedule(Runnable runnable, Schedule when) {
		return schedule(null, runnable, when);
	}

	public Job schedule(String name, Runnable runnable, Schedule when) {
		Objects.requireNonNull(runnable, "Runnable must not be null");
		Objects.requireNonNull(when, "Schedule must not be null");

		// if name is null, set job name to the runnable...
		String jobName = name == null ? runnable.toString() : name;

		Job job = prepareJob(jobName, runnable, when);
		long currentTimeInMillis = System.currentTimeMillis();
		if(when.nextExecutionInMillis(
			currentTimeInMillis,
			job.executionsCount(),
			job.lastExecutionEndedTimeInMillis())
				< currentTimeInMillis) {
			logger.warn("The job '{}' is scheduled for a past time, it will not be executed", jobName);
		}

		logger.info("Scheduling job '{}' to run at {}", job.name(), job.schedule());
		scheduleNextExecution(job);

		return job;
	}

	public Collection<Job> jobStatus() {
		return indexedJobsByName.values();
	}

	public Optional<Job> findJob(String name) {
		return Optional.ofNullable(indexedJobsByName.get(name));
	}

	public CompletionStage<Job> cancelJob(String jobName) {
		Job job = findJob(jobName).orElseThrow(IllegalArgumentException::new);

		synchronized (this) {
			JobStatus jobStatus = job.status();
			if(jobStatus == JobStatus.DONE) {
				return CompletableFuture.completedFuture(job);
			}
			CompletableFuture<Job> existingHandle = cancelHandles.get(jobName);
			if(existingHandle != null) {
				return existingHandle;
			}

			job.schedule(Schedule.willNeverBeExecuted);
			if(jobStatus == JobStatus.READY && threadPoolExecutor.remove(job.runningJob())) {
				scheduleNextExecution(job);
				return CompletableFuture.completedFuture(job);
			}

			// if a job is READY or RUNNING wait for it to finish
			if(jobStatus == JobStatus.RUNNING
				|| jobStatus == JobStatus.READY) {
				CompletableFuture<Job> promise = new CompletableFuture<>();
				cancelHandles.put(jobName, promise);
				return promise;
			} else {
				for (Iterator<Job> iterator = nextExecutionsOrder.iterator(); iterator.hasNext();) {
					Job nextJob = iterator.next();
					if(nextJob == job) {
						iterator.remove();
						job.status(JobStatus.DONE);
						return CompletableFuture.completedFuture(job);
					}
				}
				throw new IllegalStateException("Cannot find the job "
						+ job + " in " + nextExecutionsOrder);
			}
		}
	}


	public void gracefullyShutdown() {
		gracefullyShutdown(Duration.ofSeconds(10));
	}

	/**
	 * Waits until the current running jobs are executed
	 * Cancels jobs that are planned to be executed.
	 * @param timeout The maximum time to wait
	 * @throws InterruptedException if the shutdown lasts more than 10 seconds
	 */
	@SneakyThrows
	public void gracefullyShutdown(Duration timeout) {
		logger.info("Shutting down...");

		if(!shuttingDown) {
			synchronized (this) {
				shuttingDown = true;
				threadPoolExecutor.shutdown();
			}

			// stops jobs that have not yet started to be executed
			for(Job job : jobStatus()) {
				Runnable runningJob = job.runningJob();
				if(runningJob != null) {
					threadPoolExecutor.remove(runningJob);
				}
				job.status(JobStatus.DONE);
			}
			synchronized (launcherNotifier) {
				launcherNotifier.set(false);
				launcherNotifier.notify();
			}
		}

		threadPoolExecutor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Fetch statistics about the current
	 */
	public SchedulerStats stats() {
		System.out.println(this.indexedJobsByName);
		int activeThreads = threadPoolExecutor.getActiveCount();
		return SchedulerStats.of(this.indexedJobsByName.size(), ThreadPoolStats.of(
			threadPoolExecutor.getCorePoolSize(),
			threadPoolExecutor.getMaximumPoolSize(),
			activeThreads,
			threadPoolExecutor.getPoolSize() - activeThreads,
			threadPoolExecutor.getLargestPoolSize()
		));
	}

	private Job prepareJob(String name, Runnable runnable, Schedule when) {
		// prevent jobs with the same name to be submitted at the same time
		synchronized (indexedJobsByName) {
			Job lastJob = findJob(name).orElse(null);

			if(lastJob != null && lastJob.status() != JobStatus.DONE) {
				throw new IllegalArgumentException("A job is already scheduled with the name:" + name);
			}

			Job job = new Job(
				JobStatus.DONE,
				0L,
				lastJob != null ? lastJob.executionsCount() : 0,
				lastJob != null ? lastJob.lastExecutionStartedTimeInMillis() : null,
				lastJob != null ? lastJob.lastExecutionEndedTimeInMillis() : null,
				name,
				when,
				runnable
			);
			indexedJobsByName.put(name, job);

			return job;
		}
	}

	private synchronized void scheduleNextExecution(Job job) {
		// clean up
		job.runningJob(null);

		// next execution time calculation
		long currentTimeInMillis = System.currentTimeMillis();
		try {
			job.nextExecutionTimeInMillis(
				job.schedule().nextExecutionInMillis(
					currentTimeInMillis, job.executionsCount(), job.lastExecutionEndedTimeInMillis()
				)
			);
		} catch (Throwable t) {
			logger.error("An exception found during the job next execution time calculation, " +
					"therefore the job '{}' will not be executed again.",job.name(),t);
			job.nextExecutionTimeInMillis(Schedule.WILL_NOT_BE_EXECUTED_AGAIN);
		}

		// next execution planning
		if(job.nextExecutionTimeInMillis() >= currentTimeInMillis) {
			job.status(JobStatus.SCHEDULED);
			nextExecutionsOrder.add(job);
			nextExecutionsOrder.sort(Comparator.comparing(
				Job::nextExecutionTimeInMillis
			));

			synchronized (launcherNotifier) {
				launcherNotifier.set(false);
				launcherNotifier.notify();
			}
		} else {
			logger.info("Job '{}' will not be executed again since its next execution time, " +
					"{}ms, is planned in the past", job.name(),
					Instant.ofEpochMilli(job.nextExecutionTimeInMillis()));
			job.status(JobStatus.DONE);

			CompletableFuture<Job> cancelHandle = cancelHandles.remove(job.name());
			if(cancelHandle != null) {
				cancelHandle.complete(job);
			}
		}
	}

	/**
	 * This is a daemon that in charge of adding jobs to the thread pool
	 * when they are ready to be executed.
	 */
	@SneakyThrows
	private void launcher() {
		while(!shuttingDown) {
			Long timeBeforeNextExecution = null;
			synchronized (this) {
				if(nextExecutionsOrder.size() > 0) {
					timeBeforeNextExecution = nextExecutionsOrder.get(0).nextExecutionTimeInMillis()
						- System.currentTimeMillis();
				}
			}

			if(timeBeforeNextExecution == null || timeBeforeNextExecution > 0L) {
				synchronized (launcherNotifier) {
					if(shuttingDown) {
						return;
					}
					// The launcher must check again the next job to execute.
					// This is done to make sure that changes are not ignored
					if(launcherNotifier.get()) {
						if(timeBeforeNextExecution == null) {
							launcherNotifier.wait();
						} else {
							launcherNotifier.wait(timeBeforeNextExecution);
						}
					}
					launcherNotifier.set(true);
				}
			} else {
				synchronized (this) {
					if(shuttingDown) {
						return;
					}

					if(nextExecutionsOrder.size() > 0) {
						Job jobToRun = nextExecutionsOrder.remove(0);
						jobToRun.status(JobStatus.READY);
						jobToRun.runningJob(() -> runJob(jobToRun));
						if(threadPoolExecutor.getActiveCount() == threadPoolExecutor.getMaximumPoolSize()) {
							logger.warn("Job thread pool is full, either tasks take too much " +
									"time to execute or thread pool size is too low");
						}
						threadPoolExecutor.execute(jobToRun.runningJob());
					}
				}
			}
		}
	}

	/**
	 * The wrapper around a job that will be executed in the thread pool.
	 * It is especially in charge of logging, changing the job status
	 * and checking for the next job to be executed.
	 * @param jobToRun the job to execute
	 */
	private void runJob(Job jobToRun) {
		long startExecutionTime = System.currentTimeMillis();
		long timeBeforeNextExecution = jobToRun.nextExecutionTimeInMillis() - startExecutionTime;
		if(timeBeforeNextExecution < 0) {
			logger.debug("Job '{}' execution is {}ms late", jobToRun.name(), -timeBeforeNextExecution);
		}
		jobToRun.status(JobStatus.RUNNING);
		jobToRun.lastExecutionStartedTimeInMillis(startExecutionTime);
		jobToRun.threadRunningJob(Thread.currentThread());

		try {
			jobToRun.runnable().run();
		} catch(Throwable t) {
			logger.error("Error during job '{}' execution", jobToRun.name(), t);
		}
		jobToRun.executionsCount(jobToRun.executionsCount() + 1);
		jobToRun.lastExecutionEndedTimeInMillis(System.currentTimeMillis());
		jobToRun.threadRunningJob(null);

		if(logger.isDebugEnabled()) {
			logger.debug("Job '{}' executed in {}ms",
					jobToRun.name(), System.currentTimeMillis() - startExecutionTime);
		}

		if(shuttingDown) {
			return;
		}
		synchronized (this) {
			scheduleNextExecution(jobToRun);
		}
	}

	private static class ThreadFactory implements java.util.concurrent.ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Panoply Scheduler Worker #" + threadCounter.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}

}
