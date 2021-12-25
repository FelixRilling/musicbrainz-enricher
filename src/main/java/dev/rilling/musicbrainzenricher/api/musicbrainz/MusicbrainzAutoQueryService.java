package dev.rilling.musicbrainzenricher.api.musicbrainz;

import dev.rilling.musicbrainzenricher.core.ReleaseGroupRepository;
import dev.rilling.musicbrainzenricher.core.ReleaseRepository;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Consumer;

@Service
@ThreadSafe
public class MusicbrainzAutoQueryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MusicbrainzAutoQueryService.class);

	private static final int LIMIT = 1000;

	private final ReleaseRepository releaseRepository;
	private final ReleaseGroupRepository releaseGroupRepository;

	MusicbrainzAutoQueryService(ReleaseRepository releaseRepository, ReleaseGroupRepository releaseGroupRepository) {
		this.releaseRepository = releaseRepository;
		this.releaseGroupRepository = releaseGroupRepository;
	}

	public void autoQueryReleasesWithRelationships(@NotNull Consumer<UUID> mbidConsumer) {
		long count = releaseRepository.countReleasesWhereRelationshipsExist();
		LOGGER.info("Found a total of {} releases with at least one relationship.", count);

		long offset = 0;
		while (offset < count) {
			LOGGER.info("Loading {} releases with offset {} with at least one relationship...", LIMIT, offset);
			releaseRepository.findReleaseMbidWhereRelationshipsExist(offset, LIMIT).forEach(mbidConsumer);
			LOGGER.info("Loaded {} releases with offset {} with at least one relationship...", LIMIT, offset);
			offset += LIMIT;
		}
	}

	public void autoQueryReleaseGroupsWithRelationships(@NotNull Consumer<UUID> mbidConsumer) {
		long count = releaseGroupRepository.countReleaseGroupsWhereRelationshipsExist();
		LOGGER.info("Found a total of {} release groups with at least one relationship.", count);

		long offset = 0;
		while (offset < count) {
			LOGGER.info("Loading {} release groups with offset {} with at least one relationship...", LIMIT, offset);
			releaseGroupRepository.findReleaseGroupsMbidWhereRelationshipsExist(offset, LIMIT).forEach(mbidConsumer);
			offset += LIMIT;
		}
	}

}