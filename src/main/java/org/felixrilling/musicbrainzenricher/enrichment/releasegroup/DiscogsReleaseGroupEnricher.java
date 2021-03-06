package org.felixrilling.musicbrainzenricher.enrichment.releasegroup;

import org.felixrilling.musicbrainzenricher.api.discogs.DiscogsMaster;
import org.felixrilling.musicbrainzenricher.api.discogs.DiscogsQueryService;
import org.felixrilling.musicbrainzenricher.core.DataType;
import org.felixrilling.musicbrainzenricher.core.genre.GenreMatcherService;
import org.felixrilling.musicbrainzenricher.enrichment.GenreEnricher;
import org.felixrilling.musicbrainzenricher.util.RegexUtils;
import org.jetbrains.annotations.NotNull;
import org.musicbrainz.model.RelationWs2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

// https://musicbrainz.org/release-group/a63e5fa6-d6ad-47bd-986d-4a27b0c9de70
// https://www.discogs.com/master/1381500
@Service
class DiscogsReleaseGroupEnricher implements GenreEnricher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscogsReleaseGroupEnricher.class);

    private static final Pattern URL_REGEX = Pattern.compile("https?://www\\.discogs\\.com/master/(?<id>\\d+)");

    private final GenreMatcherService genreMatcherService;
    private final DiscogsQueryService discogsQueryService;

    DiscogsReleaseGroupEnricher(GenreMatcherService genreMatcherService, DiscogsQueryService discogsQueryService) {
        this.genreMatcherService = genreMatcherService;
        this.discogsQueryService = discogsQueryService;
    }

    @Override
    public @NotNull Set<String> fetchGenres(@NotNull RelationWs2 relation) {
        Optional<String> discogsId = RegexUtils.maybeGroup(URL_REGEX.matcher(relation.getTargetId()), "id");
        if (discogsId.isEmpty()) {
            LOGGER.warn("Could not find discogs ID: '{}'.", relation.getTargetId());
            return Set.of();
        }
        return discogsQueryService.lookUpMaster(discogsId.get()).map(release -> genreMatcherService.match(extractGenres(release)))
                .orElse(Set.of());
    }

    private @NotNull Set<String> extractGenres(@NotNull DiscogsMaster master) {
        Set<String> genres = new HashSet<>(master.getGenres());
        if (master.getStyles() != null) {
            genres.addAll(master.getStyles());
        }
        return genres;
    }

    @Override
    public boolean isRelationSupported(@NotNull RelationWs2 relation) {
        return "http://musicbrainz.org/ns/rel-2.0#discogs".equals(relation.getType()) && "http://musicbrainz.org/ns/rel-2.0#url"
                .equals(relation.getTargetType());
    }


    @Override
    public @NotNull DataType getDataType() {
        return DataType.RELEASE_GROUP;
    }
}
