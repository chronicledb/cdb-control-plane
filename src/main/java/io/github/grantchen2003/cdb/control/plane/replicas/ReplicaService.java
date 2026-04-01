package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReplicaService {
    private final Ec2Client ec2Client;
    private final ReplicaRepository replicaRepository;

    public ReplicaService(Ec2Client ec2Client, ReplicaRepository replicaRepository) {
        this.ec2Client = ec2Client;
        this.replicaRepository = replicaRepository;
    }

    public Replica createReplica(String userId, String chronicleName, ReplicaType replicaType) {
        final Replica replica = new Replica(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                replicaType,
                null,
                null,
                null,
                null,
                ReplicaStatus.NEW,
                Instant.now()
        );

        replicaRepository.save(replica);

        return replica;
    }

    public Optional<Replica> findById(String replicaId) {
        return replicaRepository.findById(replicaId);
    }

    public void delete(Replica replica) {
        ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(
                        replica.applierInstanceId(),
                        replica.storageEngineInstanceId(),
                        replica.txManagerInstanceId()
                )
                .build());

        replicaRepository.deleteById(replica.id());
    }
}