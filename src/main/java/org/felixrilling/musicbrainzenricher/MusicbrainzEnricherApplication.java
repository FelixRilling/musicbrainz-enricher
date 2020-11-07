package org.felixrilling.musicbrainzenricher;

import org.felixrilling.musicbrainzenricher.musicbrainz.MusicbrainzQueryService;
import org.felixrilling.musicbrainzenricher.release.ReleaseEnricherService;
import org.musicbrainz.includes.ArtistIncludesWs2;
import org.musicbrainz.model.entity.ArtistWs2;
import org.musicbrainz.model.entity.ReleaseWs2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class MusicbrainzEnricherApplication implements CommandLineRunner {

    private final ReleaseEnricherService releaseEnricherService;
    private final MusicbrainzQueryService musicbrainzQueryService;

    MusicbrainzEnricherApplication(ReleaseEnricherService releaseEnricherService, MusicbrainzQueryService musicbrainzQueryService) {
        this.releaseEnricherService = releaseEnricherService;
        this.musicbrainzQueryService = musicbrainzQueryService;
    }

    public static void main(String[] args) {
        SpringApplication.run(MusicbrainzEnricherApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ArtistIncludesWs2 includes = new ArtistIncludesWs2();
        includes.includeAll();
        ArtistWs2 artistWs2 = musicbrainzQueryService.lookUpArtist("947eaa1e-92e7-4dc3-9529-5cc6d3396e5d", includes);
        for (ReleaseWs2 release : artistWs2.getReleases()) {
            releaseEnricherService.enrichRelease(release.getId());
            Thread.sleep(1000L);
        }
    }
}
