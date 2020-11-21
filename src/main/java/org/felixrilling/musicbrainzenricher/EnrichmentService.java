package org.felixrilling.musicbrainzenricher;

import org.felixrilling.musicbrainzenricher.api.musicbrainz.MusicbrainzDbQueryService;
import org.felixrilling.musicbrainzenricher.api.musicbrainz.MusicbrainzQueryService;
import org.felixrilling.musicbrainzenricher.api.musicbrainz.QueryException;
import org.felixrilling.musicbrainzenricher.history.HistoryService;
import org.felixrilling.musicbrainzenricher.enrichment.release.ReleaseEnricherService;
import org.jetbrains.annotations.NotNull;
import org.musicbrainz.includes.ReleaseIncludesWs2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
class EnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentService.class);

    private final ReleaseEnricherService releaseEnricherService;
    private final HistoryService historyService;
    private final ApplicationContext applicationContext;

    EnrichmentService(ReleaseEnricherService releaseEnricherService, HistoryService historyService, ApplicationContext applicationContext) {
        this.releaseEnricherService = releaseEnricherService;
        this.historyService = historyService;
        this.applicationContext = applicationContext;
    }

    public void runInDumpMode(@NotNull DataType dataType) {
        MusicbrainzDbQueryService musicbrainzDbQueryService = applicationContext.getBean(MusicbrainzDbQueryService.class);

        if (dataType == DataType.RELEASE) {
            enrichRelease(consumer -> {
                musicbrainzDbQueryService.queryReleasesWithRelationships(consumer);
                return null;
            });
        }
    }

    public void runInQueryMode(@NotNull DataType dataType, @NotNull String query) {
        MusicbrainzQueryService musicbrainzQueryService = applicationContext.getBean(MusicbrainzQueryService.class);

        if (dataType == DataType.RELEASE) {
            enrichRelease(consumer -> {
                ReleaseIncludesWs2 includes = new ReleaseIncludesWs2();
                includes.excludeAll();
                musicbrainzQueryService.queryReleases(query, consumer, includes);
                return null;
            });
        }
    }

    private void enrichRelease(@NotNull ConsumerBinder<String, QueryException> consumerBinder) {
        try {
            consumerBinder.bind(mbid -> {
                if (!historyService.checkIsDue(DataType.RELEASE, mbid)) {
                    logger.debug("Check is not due for '{}', skipping.", mbid);
                    return;
                }
                try {
                    releaseEnricherService.enrichRelease(mbid);
                    historyService.markAsChecked(DataType.RELEASE, mbid);
                } catch (QueryException e) {
                    logger.error("Could not enrich release.", e);
                }
            }).execute();
        } catch (QueryException e) {
            logger.error("Could not query releases.", e);
        }
    }

    @FunctionalInterface
    interface ConsumerBinder<TConsumerValue, EException extends Throwable> {
        NoOp<EException> bind(@NotNull Consumer<TConsumerValue> consumer) throws EException;

        @FunctionalInterface
        interface NoOp<E extends Throwable> {
            void execute() throws E;
        }
    }
}