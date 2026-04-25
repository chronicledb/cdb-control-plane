package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.chronicles.Chronicle;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.config.replica.ReplicaConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReplicaService {

    private final ChronicleService chronicleService;
    private final Ec2Client ec2Client;
    private final ReplicaRepository replicaRepository;
    private final ReplicaConfig replicaConfig;

    public ReplicaService(
            ChronicleService chronicleService,
            Ec2Client ec2Client,
            ReplicaRepository replicaRepository,
            ReplicaConfig replicaConfig) {
        this.chronicleService = chronicleService;
        this.ec2Client = ec2Client;
        this.replicaRepository = replicaRepository;
        this.replicaConfig = replicaConfig;
    }

    public Replica createReplica(String userId, String chronicleName, String replicaTypeStr) {
        final Optional<Chronicle> chronicleOpt = chronicleService.findByUserIdAndName(userId, chronicleName);
        if (chronicleOpt.isEmpty()) {
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
                chronicleOpt.get().id(),
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

    public List<String> getRunningReplicaEndpoints(List<String> replicaIds) {
        return replicaRepository.findByIds(replicaIds)
                .stream()
                .filter(r -> r.status() == ReplicaStatus.RUNNING)
                .filter(r -> r.txManagerPublicIp() != null)
                .map(r -> r.txManagerPublicIp() + ":" + replicaConfig.txManagerPort())
                .toList();
    }
}