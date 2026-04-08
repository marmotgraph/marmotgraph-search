package org.marmotgraph.search.indexing.configuration;

import lombok.Getter;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.ErrorReportResult;
import org.marmotgraph.search.common.utils.translation.TranslatorRegistry;
import org.marmotgraph.search.indexing.controller.indexing.IndexingController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexingScheduler {

    private final IndexingController indexingController;
    private final TranslatorRegistry translatorRegistry;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String inProgressInterval;
    private final String releasedInterval;
    private final String inProgressAutoReleaseInterval;
    private final String releasedAutoReleaseInterval;
    @Getter
    private final Map<IndexingMode, ErrorReportResult> errorReports = new ConcurrentHashMap<>();

    public enum IndexingMode {
        IN_PROGRESS,
        IN_PROGRESS_AUTORELEASE,
        RELEASED,
        RELEASED_AUTORELEASE
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(4);
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    public IndexingScheduler(IndexingController indexingController, TranslatorRegistry translatorRegistry,
                             @Value("${indexing.inprogress:3600000}") String inProgressInterval,
                             @Value("${indexing.released:3600000}") String releasedInterval,
                             @Value("${indexing.inprogress-autorelease:3600000}") String inProgressAutoreleaseInterval,
                             @Value("${indexing.released-autorelease:3600000}") String releasedAutoreleaseInterval) {
        this.indexingController = indexingController;
        this.inProgressInterval = inProgressInterval;
        this.translatorRegistry = translatorRegistry;
        this.releasedInterval = releasedInterval;
        this.inProgressAutoReleaseInterval = inProgressAutoreleaseInterval;
        this.releasedAutoReleaseInterval = releasedAutoreleaseInterval;
    }

    private void scheduledIndexing(IndexingMode mode, String interval){
        DataStage stage = switch (mode) {
            case IN_PROGRESS, IN_PROGRESS_AUTORELEASE -> DataStage.IN_PROGRESS;
            default -> DataStage.RELEASED;
        };
        boolean isAutorelease = switch (mode){
            case IN_PROGRESS_AUTORELEASE, RELEASED_AUTORELEASE ->  true;
            default -> false;
        };
        logger.info("Starting scheduled indexing for stage \"{}\" (autorelease: {}) - interval: {}ms", stage.name(), isAutorelease, interval);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ErrorReportResult.Extended result = new ErrorReportResult.Extended();
        result.setErrorsByTarget(translatorRegistry.getTranslators().stream().filter(m -> m.autoRelease() == isAutorelease).map(m -> indexingController.populateIndex(m, stage, false)).filter(Objects::nonNull).toList());
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        Duration duration = Duration.between(start, end);
        result.setStartedAt(start.format( DateTimeFormatter.ISO_INSTANT ));
        result.setEndedAt(end.format( DateTimeFormatter.ISO_INSTANT ));
        result.setDuration(duration.toMillis());
        this.errorReports.put(mode, result);
        logger.info("Scheduled indexing for stage  \"{}\" (autorelease: {}) completed in {}", stage.name(), isAutorelease, duration);
    }


    @Scheduled(fixedDelayString = "${indexing.inprogress:3600000}")
    public void scheduleInProgressIndexing(){
        scheduledIndexing(IndexingMode.IN_PROGRESS, inProgressInterval);
    }

    @Scheduled(fixedDelayString = "${indexing.released:3600000}")
    public void scheduleReleasedIndexing(){
        scheduledIndexing(IndexingMode.RELEASED, releasedInterval);
    }

    @Scheduled(fixedDelayString = "${indexing.inprogress-autorelease:86400000}")
    public void scheduleInProgressAutoReleaseIndexing(){
        scheduledIndexing(IndexingMode.IN_PROGRESS_AUTORELEASE, inProgressAutoReleaseInterval);
    }

    @Scheduled(fixedDelayString = "${indexing.released-autorelease:86400000}")
    public void scheduleReleasedAutoReleaseIndexing(){
        scheduledIndexing(IndexingMode.RELEASED_AUTORELEASE, releasedAutoReleaseInterval);
    }
}
