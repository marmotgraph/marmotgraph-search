package org.marmotgraph.search.indexing.configuration;

import lombok.Getter;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.ErrorReportResult;
import org.marmotgraph.search.common.utils.translation.TranslatorRegistry;
import org.marmotgraph.search.indexing.controller.indexing.IndexingController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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

    public IndexingScheduler(IndexingController indexingController, TranslatorRegistry translatorRegistry) {
        this.indexingController = indexingController;
        this.translatorRegistry = translatorRegistry;
    }

    private void scheduledIndexing(IndexingMode mode){
        DataStage stage = switch (mode) {
            case IN_PROGRESS, IN_PROGRESS_AUTORELEASE -> DataStage.IN_PROGRESS;
            default -> DataStage.RELEASED;
        };
        boolean isAutorelease = switch (mode){
            case IN_PROGRESS_AUTORELEASE, RELEASED_AUTORELEASE ->  true;
            default -> false;
        };
        indexingController.recreateIdentifiersIndex(stage, false);
        logger.info("Starting scheduled indexing for stage \"{}\" (autorelease: {})", stage.name(), isAutorelease);
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC);
        ErrorReportResult.Extended result = new ErrorReportResult.Extended();
        result.setErrorsByTarget(translatorRegistry.getTranslators().stream().filter(m -> m.autoRelease() == isAutorelease).map(m ->
        {
            indexingController.recreateIndex(stage, m.targetClass(), m.autoRelease(), false, false); //Ensures the creation of the index if it doesn't exist yet
            return indexingController.populateIndex(m, stage, false);
        }).filter(Objects::nonNull).toList());
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        Duration duration = Duration.between(start, end);
        result.setStartedAt(start.format( DateTimeFormatter.ISO_INSTANT ));
        result.setEndedAt(end.format( DateTimeFormatter.ISO_INSTANT ));
        result.setDuration(duration.toMillis());
        this.errorReports.put(mode, result);
        logger.info("Scheduled indexing for stage  \"{}\" (autorelease: {}) completed in {}", stage.name(), isAutorelease, duration);
    }


    @Scheduled(fixedDelayString = "${indexing.interval:3600000}", initialDelayString = "${indexing.interval:3600000}")
    public void scheduleReleasedIndexing(){
        scheduledIndexing(IndexingMode.RELEASED);
        scheduledIndexing(IndexingMode.IN_PROGRESS);
    }

    @Scheduled(cron = "${indexing.autorelease-cron:0 0 1 * * *}")
    public void scheduleReleasedAutoReleaseIndexing(){
        scheduledIndexing(IndexingMode.RELEASED_AUTORELEASE);
        scheduledIndexing(IndexingMode.IN_PROGRESS_AUTORELEASE);
    }
}
