package io.github.grantchen2003.cdb.control.plane.replicas;

import java.time.Instant;

public record Replica(
        String id,
        String userId,
        String chronicleName,
        ReplicaType type,
        String applierInstanceId,
        String storageEngineInstanceId,
        String txManagerInstanceId,
        String txManagerPublicIp,
        ReplicaStatus status,
        Instant createdAt
) {
    public Replica withApplierInstanceId(String applierInstanceId) {
        return new Replica(
                id,
                userId,
                chronicleName,
                type,
                applierInstanceId,
                storageEngineInstanceId,
                txManagerInstanceId,
                txManagerPublicIp,
                status,
                createdAt
        );
    }

    public Replica withStorageEngineInstanceId(String storageEngineInstanceId) {
        return new Replica(
                id,
                userId,
                chronicleName,
                type,
                applierInstanceId,
                storageEngineInstanceId,
                txManagerInstanceId,
                txManagerPublicIp,
                status,
                createdAt
        );
    }

    public Replica withTxManagerInstanceId(String txManagerInstanceId) {
        return new Replica(
                id,
                userId,
                chronicleName,
                type,
                applierInstanceId,
                storageEngineInstanceId,
                txManagerInstanceId,
                txManagerPublicIp,
                status,
                createdAt
        );
    }

    public Replica withTxManagerPublicIp(String txManagerPublicIp) {
        return new Replica(
                id,
                userId,
                chronicleName,
                type,
                applierInstanceId,
                storageEngineInstanceId,
                txManagerInstanceId,
                txManagerPublicIp,
                status,
                createdAt
        );
    }

    public Replica withStatus(ReplicaStatus status) {
        return new Replica(
                id,
                userId,
                chronicleName,
                type,
                applierInstanceId,
                storageEngineInstanceId,
                txManagerInstanceId,
                txManagerPublicIp,
                status,
                createdAt
        );
    }
}