package org.felixrilling.musicbrainzenricher.enrichment.releasegroup;

import org.felixrilling.musicbrainzenricher.DataType;
import org.felixrilling.musicbrainzenricher.api.musicbrainz.MusicbrainzDbQueryService;
import org.felixrilling.musicbrainzenricher.api.wikidata.WikidataService;
import org.felixrilling.musicbrainzenricher.enrichment.GenreEnricher;
import org.felixrilling.musicbrainzenricher.enrichment.RegexUtils;
import org.jetbrains.annotations.NotNull;
import org.musicbrainz.model.RelationWs2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The simplified logic looks like this:
 * <ol>
 *     <li>Extract wikidata ID from URL</li>
 *     <li>Look up wikidata release entity by ID</li>
 *     <li>Find genre statements</li>
 *     <li>For every genre statement, get its id</li>
 *     <li>For every genre id, find the genre entity by ID</li>
 *     <li>For every genre entity, find the musicbrainz link statement</li>
 *     <li>For every genre entities musicbrainz link statement, look up its name by the MBID against the musicbrainz database</li>
 *     <li>Return the names found in the musicbrainz database</li>
 * </ol>
 */
// https://www.wikidata.org/wiki/Q922756
// https://www.mediawiki.org/wiki/Wikidata_Toolkit
@Service
class WikidataReleaseGroupEnricher implements GenreEnricher {

    private static final Logger logger = LoggerFactory.getLogger(WikidataReleaseGroupEnricher.class);

    private static final String GENRE_PROPERTY_ID = "P136";
    private static final String MUSICBRAINZ_LINK_PROPERTY_ID = "P8052";

    private static final Pattern ID_REGEX = Pattern.compile(".+/(?<id>Q\\d+)$");

    private final WikidataService wikidataService;
    private final MusicbrainzDbQueryService musicbrainzDbQueryService;

    WikidataReleaseGroupEnricher(WikidataService wikidataService, MusicbrainzDbQueryService musicbrainzDbQueryService) {
        this.wikidataService = wikidataService;
        this.musicbrainzDbQueryService = musicbrainzDbQueryService;
    }

    @Override
    public @NotNull Set<String> fetchGenres(@NotNull RelationWs2 relation) {
        Optional<String> id = RegexUtils.maybeGroup(ID_REGEX.matcher(relation.getTargetId()), "id");
        if (id.isEmpty()) {
            logger.warn("Could not find ID in '{}'.", relation.getTargetId());
            return Set.of();
        }
        // We can skip genre matching as we use the genre names directly from Musicbrainz.
        return wikidataService.findEntityPropertyValues(id.get(), GENRE_PROPERTY_ID).map(this::extractGenreNames).orElse(Set.of());
    }

    private @NotNull Set<String> extractGenreNames(@NotNull List<Statement> genreStatements) {
        Set<String> genres = new HashSet<>();
        for (Statement genreStatement : genreStatements) {
            if (!(genreStatement.getValue() instanceof EntityIdValue)) {
                logger.warn("Unexpected genre statement type: '{}'.", genreStatement);
            } else {
                findGenreName(((EntityIdValue) genreStatement.getValue()).getId()).ifPresent(genres::add);
            }
        }
        return genres;
    }

    private @NotNull Optional<String> findGenreName(@NotNull String genreId) {
        Optional<List<Statement>> musicbrainzLinkStatements = wikidataService
                .findEntityPropertyValues(genreId, MUSICBRAINZ_LINK_PROPERTY_ID);
        if (musicbrainzLinkStatements.map(List::isEmpty).orElse(true)) {
            logger.warn("No musicbrainz link found for genre: '{}'.", genreId);
            return Optional.empty();
        }
        Value value = musicbrainzLinkStatements.get().get(0).getValue();
        if (!(value instanceof StringValue)) {
            logger.warn("Unexpected musicbrainz link type: '{}'.", value);
            return Optional.empty();
        }
        String mbid = ((StringValue) value).getString();
        return musicbrainzDbQueryService.queryGenreNameByMbid(mbid);
    }

    @Override
    public boolean relationSupported(@NotNull RelationWs2 relation) {
        return "http://musicbrainz.org/ns/rel-2.0#wikidata".equals(relation.getType()) && "http://musicbrainz.org/ns/rel-2.0#url"
                .equals(relation.getTargetType());
    }

    @Override
    public @NotNull DataType getDataType() {
        return DataType.RELEASE_GROUP;
    }
}
