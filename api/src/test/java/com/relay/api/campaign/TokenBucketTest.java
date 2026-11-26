package com.relay.api.campaign;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void allowsBurstThenRefills() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(2, 10);

        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isTrue();
        assertThat(bucket.tryAcquire()).isFalse();

        Thread.sleep(220);
        assertThat(bucket.tryAcquire()).isTrue();
    }

    @Test
    void timesOutWhenTheBucketStaysEmpty() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(1, 0.1);

        assertThat(bucket.acquire(50)).isTrue();
        assertThat(bucket.acquire(50)).isFalse();
    }
}
