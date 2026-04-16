package io.github.grantchen2003.cdb.control.plane.replicas;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ReplicaTest {

    private final Replica replica = new Replica(
            "replica-1",
            "user-1",
            "chronicle-1",
            "my-chronicle",
            ReplicaType.REDIS,
            "i-applier-123",
            "i-storage-123",
            "i-txmanager-123",
            null,
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Test
    void withApplierInstanceId_returnsNewReplicaWithApplierInstanceId() {
        final Replica updated = replica.withApplierInstanceId("i-new-applier");

        assertThat(updated.applierInstanceId()).isEqualTo("i-new-applier");
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.storageEngineInstanceId()).isEqualTo(replica.storageEngineInstanceId());
        assertThat(updated.txManagerInstanceId()).isEqualTo(replica.txManagerInstanceId());
    }

    @Test
    void withApplierInstanceId_doesNotMutateOriginal() {
        replica.withApplierInstanceId("i-new-applier");
        assertThat(replica.applierInstanceId()).isEqualTo("i-applier-123");
    }

    @Test
    void withStorageEngineInstanceId_returnsNewReplicaWithStorageEngineInstanceId() {
        final Replica updated = replica.withStorageEngineInstanceId("i-new-storage");

        assertThat(updated.storageEngineInstanceId()).isEqualTo("i-new-storage");
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.applierInstanceId()).isEqualTo(replica.applierInstanceId());
        assertThat(updated.txManagerInstanceId()).isEqualTo(replica.txManagerInstanceId());
    }

    @Test
    void withStorageEngineInstanceId_doesNotMutateOriginal() {
        replica.withStorageEngineInstanceId("i-new-storage");
        assertThat(replica.storageEngineInstanceId()).isEqualTo("i-storage-123");
    }

    @Test
    void withTxManagerInstanceId_returnsNewReplicaWithTxManagerInstanceId() {
        final Replica updated = replica.withTxManagerInstanceId("i-new-txmanager");

        assertThat(updated.txManagerInstanceId()).isEqualTo("i-new-txmanager");
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.applierInstanceId()).isEqualTo(replica.applierInstanceId());
        assertThat(updated.storageEngineInstanceId()).isEqualTo(replica.storageEngineInstanceId());
    }

    @Test
    void withTxManagerInstanceId_doesNotMutateOriginal() {
        replica.withTxManagerInstanceId("i-new-txmanager");
        assertThat(replica.txManagerInstanceId()).isEqualTo("i-txmanager-123");
    }

    @Test
    void withTxManagerPublicIp_returnsNewReplicaWithTxManagerPublicIp() {
        final Replica updated = replica.withTxManagerPublicIp("203.0.113.10");

        assertThat(updated.txManagerPublicIp()).isEqualTo("203.0.113.10");
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.status()).isEqualTo(replica.status());
    }

    @Test
    void withTxManagerPublicIp_doesNotMutateOriginal() {
        replica.withTxManagerPublicIp("203.0.113.10");
        assertThat(replica.txManagerPublicIp()).isNull();
    }

    @Test
    void withStatus_returnsNewReplicaWithStatus() {
        final Replica updated = replica.withStatus(ReplicaStatus.RUNNING);

        assertThat(updated.status()).isEqualTo(ReplicaStatus.RUNNING);
        assertThat(updated.id()).isEqualTo(replica.id());
        assertThat(updated.txManagerPublicIp()).isEqualTo(replica.txManagerPublicIp());
    }

    @Test
    void withStatus_doesNotMutateOriginal() {
        replica.withStatus(ReplicaStatus.RUNNING);
        assertThat(replica.status()).isEqualTo(ReplicaStatus.PROVISIONING);
    }
}