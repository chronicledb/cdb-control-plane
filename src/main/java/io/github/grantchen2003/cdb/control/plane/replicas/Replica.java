package io.github.grantchen2003.cdb.control.plane.replicas;

import java.time.Instant;

public record Replica(
        String id,
        String userId,
        String chronicleName,
        ReplicaType type,
        String ec2InstanceId,
        String publicIp,
        ReplicaStatus status,
        Instant createdAt
) {
    public Replica withPublicIp(String publicIp) {
        return new Replica(id, userId, chronicleName, type, ec2InstanceId, publicIp, status, createdAt);
    }

    public Replica withStatus(ReplicaStatus status) {
        return new Replica(id, userId, chronicleName, type, ec2InstanceId, publicIp, status, createdAt);
    }
}
