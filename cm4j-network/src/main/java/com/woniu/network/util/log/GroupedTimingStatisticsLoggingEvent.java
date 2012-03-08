package com.woniu.network.util.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;

public class GroupedTimingStatisticsLoggingEvent extends LoggingEvent {
    private GroupedTimingStatistics statistics;

    public GroupedTimingStatisticsLoggingEvent(String loggerName, Logger logger, Level level,
                                               String message, GroupedTimingStatistics statistics) {
        super(loggerName, logger, level, message, null, null);
        this.statistics = statistics;
    }

    public GroupedTimingStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(GroupedTimingStatistics statistics) {
        this.statistics = statistics;
    }
}
