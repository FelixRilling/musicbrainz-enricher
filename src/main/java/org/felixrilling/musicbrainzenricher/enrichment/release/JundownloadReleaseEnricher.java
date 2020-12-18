package org.felixrilling.musicbrainzenricher.enrichment.release;

import org.felixrilling.musicbrainzenricher.api.ScrapingService;
import org.felixrilling.musicbrainzenricher.core.DataType;
import org.felixrilling.musicbrainzenricher.core.genre.GenreMatcherService;
import org.felixrilling.musicbrainzenricher.enrichment.GenreEnricher;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;
import org.musicbrainz.model.RelationWs2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Uses web scraping because the regular API does not seem to be documented.
 */
// https://musicbrainz.org/release/4a7262b6-a64d-4214-ae61-bb16d15d724c
// https://www.junodownload.com/products/indivision-mount-vesuvius-newborn-star/4144821-02/
@Service
class JundownloadReleaseEnricher implements GenreEnricher {

    private static final Logger logger = LoggerFactory.getLogger(JundownloadReleaseEnricher.class);

    private static final Pattern HOST_REGEX = Pattern.compile("www\\.junodownload\\.com");
    private static final Evaluator TAG_QUERY = QueryParser.parse("[itemprop='genre']");

    private final GenreMatcherService genreMatcherService;
    private final ScrapingService scrapingService;

    JundownloadReleaseEnricher(GenreMatcherService genreMatcherService, ScrapingService scrapingService) {
        this.genreMatcherService = genreMatcherService;
        this.scrapingService = scrapingService;
    }

    @Override
    public @NotNull Set<String> fetchGenres(@NotNull RelationWs2 relation) {
        return scrapingService.load(relation.getTargetId()).map(this::extractTags).map(genreMatcherService::match).orElse(Set.of());
    }

    private @NotNull Set<String> extractTags(@NotNull Document document) {
        return Set.of(document.select(TAG_QUERY).attr("content"));
    }

    @Override
    public boolean relationSupported(@NotNull RelationWs2 relation) {
        if (!"http://musicbrainz.org/ns/rel-2.0#url".equals(relation.getTargetType())) {
            return false;
        }
        URL url;
        try {
            url = new URL(relation.getTargetId());
        } catch (MalformedURLException e) {
            logger.warn("Could not parse as URL: '{}'.", relation.getTargetId(), e);
            return false;
        }
        return HOST_REGEX.matcher(url.getHost()).matches();
    }

    @Override
    public @NotNull DataType getDataType() {
        return DataType.RELEASE;
    }
}
