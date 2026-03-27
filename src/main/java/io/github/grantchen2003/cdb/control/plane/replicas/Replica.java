package io.github.grantchen2003.cdb.control.plane.replicas;

import java.time.Instant;

public record Replica(
        String id,
        String userId,
        String chronicleName,
        ReplicaType type,
        String ec2InstanceId,
        Instant createdAt
) {}
