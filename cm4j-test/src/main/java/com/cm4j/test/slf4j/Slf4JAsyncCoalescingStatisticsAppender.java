package com.cm4j.test.slf4j;

import java.io.Flushable;
import java.io.IOException;
import java.util.Iterator;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.helpers.GenericAsyncCoalescingStatisticsAppender;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

public class Slf4JAsyncCoalescingStatisticsAppender extends AppenderBase<ILoggingEvent> implements
		AppenderAttachable<ILoggingEvent> {
	private GenericAsyncCoalescingStatisticsAppender baseImplementation = new GenericAsyncCoalescingStatisticsAppender();
	private Level downStreamLogLevel = Level.INFO;
	private final AppenderAttachableImpl<ILoggingEvent> downStreamAppenders = new AppenderAttachableImpl<ILoggingEvent>();

	// -- AppenderAttachable methods --
	@Override
	public void addAppender(Appender<ILoggingEvent> newAppender) {
		synchronized (downStreamAppenders) {
			downStreamAppenders.addAppender(newAppender);
		}
	}

	@Override
	public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
		synchronized (downStreamAppenders) {
			return downStreamAppenders.iteratorForAppenders();
		}
	}

	@Override
	public Appender<ILoggingEvent> getAppender(String name) {
		synchronized (downStreamAppenders) {
			return downStreamAppenders.getAppender(name);
		}
	}

	@Override
	public boolean isAttached(Appender<ILoggingEvent> eAppender) {
		synchronized (downStreamAppenders) {
			return downStreamAppenders.isAttached(eAppender);
		}
	}

	@Override
	public void detachAndStopAllAppenders() {
		synchronized (downStreamAppenders) {
			downStreamAppenders.detachAndStopAllAppenders();
		}
	}

	@Override
	public boolean detachAppender(Appender<ILoggingEvent> eAppender) {
		synchronized (downStreamAppenders) {
			return downStreamAppenders.detachAppender(eAppender);
		}
	}

	@Override
	public boolean detachAppender(String name) {
		synchronized (downStreamAppenders) {
			return downStreamAppenders.detachAppender(name);
		}
	}

	// -- AppenderBase methods --
	@Override
	protected void append(ILoggingEvent eventObject) {
		baseImplementation.append(eventObject.getMessage());
	}

	@Override
	public void start() {
		baseImplementation.start(new GenericAsyncCoalescingStatisticsAppender.GroupedTimingStatisticsHandler() {
			@Override
			public void handle(GroupedTimingStatistics groupedTimingStatistics) {
				ILoggingEvent event = new GroupedTimingStatisticsLoggingEvent(Logger.FQCN, (Logger) LoggerFactory
						.getLogger(StopWatch.DEFAULT_LOGGER_NAME), getDownStreamLogLevel(), groupedTimingStatistics
						.toString(), groupedTimingStatistics);
				synchronized (downStreamAppenders) {
					try {
						downStreamAppenders.appendLoopOnAppenders(event);
					} catch (Exception ex) {
						ILoggingEvent errorEvent = new LoggingEvent(Logger.FQCN, (Logger) LoggerFactory
								.getLogger(StopWatch.DEFAULT_LOGGER_NAME), Level.ERROR,
								"Exception calling appender with GroupedTimingStatistics on downstream appender", ex,
								new Object[] { event });
						downStreamAppenders.appendLoopOnAppenders(errorEvent);
					}
				}
			}

			@Override
			public void error(String s) {
				ILoggingEvent event = new LoggingEvent(Logger.FQCN, (Logger) LoggerFactory
						.getLogger(StopWatch.DEFAULT_LOGGER_NAME), Level.ERROR, s, null, null);
				downStreamAppenders.appendLoopOnAppenders(event);
			}
		});
		started = true;
	}

	@Override
	public void stop() {
		baseImplementation.stop();
		synchronized (downStreamAppenders) {
			for (Appender<ILoggingEvent> appender : new IteratorIterable<Appender<ILoggingEvent>>(
					downStreamAppenders.iteratorForAppenders())) {
				if (appender instanceof Flushable) {
					try {
						((Flushable) appender).flush();
					} catch (IOException e) {
						// ignored, we're shutting down
					}
				}
			}
			for (Appender<ILoggingEvent> appender : new IteratorIterable<Appender<ILoggingEvent>>(
					downStreamAppenders.iteratorForAppenders())) {
				appender.stop();
			}
		}
		started = false;
	}

	@Override
	public void setName(String name) {
		super.setName(name);
		baseImplementation.setName(name);
	}

	// -- Properties
	public Level getDownStreamLogLevel() {
		return downStreamLogLevel;
	}

	public void setDownStreamLogLevel(Level downStreamLogLevel) {
		this.downStreamLogLevel = downStreamLogLevel;
	}

	public long getTimeSlice() {
		return baseImplementation.getTimeSlice();
	}

	public void setTimeSlice(long timeSlice) {
		baseImplementation.setTimeSlice(timeSlice);
	}

	public void setCreateRollupStatistics(boolean createRollupStatistics) {
		baseImplementation.setCreateRollupStatistics(createRollupStatistics);
	}

	public boolean getCreateRollupStatistics() {
		return baseImplementation.isCreateRollupStatistics();
	}

	public int getQueueSize() {
		return baseImplementation.getQueueSize();
	}

	public void setQueueSize(int queueSize) {
		baseImplementation.setQueueSize(queueSize);
	}

	public String getStopWatchParserClassName() {
		return baseImplementation.getStopWatchParserClassName();
	}

	public void setStopWatchParserClassName(String stopWatchParserClassName) {
		baseImplementation.setStopWatchParserClassName(stopWatchParserClassName);
	}

	public int getNumDiscardedMessages() {
		return baseImplementation.getNumDiscardedMessages();
	}

	class IteratorIterable<E> implements Iterable<E> {
		private Iterator<E> iterator;

		IteratorIterable(Iterator<E> iterator) {
			this.iterator = iterator;
		}

		@Override
		public Iterator<E> iterator() {
			return iterator;
		}
	}
}
