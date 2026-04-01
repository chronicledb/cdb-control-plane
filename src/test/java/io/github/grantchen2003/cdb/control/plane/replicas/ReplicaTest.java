package io.github.grantchen2003.cdb.control.plane.replicas;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReplicaTest {

    private final Replica replica = new Replica(
            "replica-1",
            "user-1",
            "my-chronicle",
            ReplicaType.REDIS,
            "i-0abc123",
            null,
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Test
    void withPublicIp_returnsNewReplicaWithPublicIp() {
        final Replica updated = replica.withPublicIp("203.0.113.10");

        assertThat(updated.publicIp()).isEqualTo("203.0.113.10");
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.status()).isEqualTo(replica.status());
    }

    @Test
    void withStatus_returnsNewReplicaWithStatus() {
        final Replica updated = replica.withStatus(ReplicaStatus.RUNNING);

        assertThat(updated.status()).isEqualTo(ReplicaStatus.RUNNING);
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.publicIp()).isEqualTo(replica.publicIp());
    }

    @Test
    void withPublicIp_doesNotMutateOriginal() {
        replica.withPublicIp("203.0.113.10");
        assertThat(replica.publicIp()).isNull();
    }

    @Test
    void withStatus_doesNotMutateOriginal() {
        replica.withStatus(ReplicaStatus.RUNNING);
        assertThat(replica.status()).isEqualTo(ReplicaStatus.PROVISIONING);
    }
}
