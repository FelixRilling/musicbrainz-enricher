package org.felixrilling.musicbrainzenricher.api.spotify;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.local.SynchronizationStrategy;
import org.felixrilling.musicbrainzenricher.api.BucketProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Scope("singleton")
class SpotifyBucketProvider implements BucketProvider {

    // https://developer.spotify.com/documentation/web-api/
    // Note: Spotify itself does not disclose an exact rate, this is only a guess to avoid running into it.
    private static final Bandwidth BANDWIDTH = Bandwidth.simple(10, Duration.ofMinutes(1));

    private final Bucket bucket = Bucket4j.builder().addLimit(BANDWIDTH).withSynchronizationStrategy(SynchronizationStrategy.LOCK_FREE).build();

    @Override
    public Bucket getBucket() {
        return bucket;
    }
}
