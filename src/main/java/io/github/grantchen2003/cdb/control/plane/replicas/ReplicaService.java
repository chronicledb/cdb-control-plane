package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReplicaService {
    private final ReplicaRepository replicaRepository;

    public ReplicaService(ReplicaRepository replicaRepository) {
        this.replicaRepository = replicaRepository;
    }

    public Replica createReplica(String userId, String chronicleName, ReplicaType replicaType) {
        // TODO: launch instance
        final String ec2InstanceId = "dummy-ec2-instance-id";

        final Replica replica = new Replica(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                replicaType,
                ec2InstanceId,
                Instant.now()
        );

        replicaRepository.save(replica);

        return replica;
    }
}
