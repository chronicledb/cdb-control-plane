package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReplicaService {

    private final ChronicleService chronicleService;
    private final Ec2Client ec2Client;
    private final ReplicaRepository replicaRepository;

    public ReplicaService(ChronicleService chronicleService, Ec2Client ec2Client, ReplicaRepository replicaRepository) {
        this.chronicleService = chronicleService;
        this.ec2Client = ec2Client;
        this.replicaRepository = replicaRepository;
    }

    public Replica createReplica(String userId, String chronicleName, String replicaTypeStr) {
        if (!chronicleService.existsByUserIdAndName(userId, chronicleName)) {
            throw new ChronicleNotFoundException();
        }

        final ReplicaType replicaType;
        try {
            replicaType = ReplicaType.valueOf(replicaTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidReplicaTypeException(replicaTypeStr);
        }

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