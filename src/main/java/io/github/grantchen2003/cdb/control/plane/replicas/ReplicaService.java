package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReplicaService {
    private final Ec2Client ec2Client;
    private final ReplicaConfig replicaConfig;
    private final ReplicaRepository replicaRepository;

    public ReplicaService(Ec2Client ec2Client, ReplicaConfig replicaConfig, ReplicaRepository replicaRepository) {
        this.ec2Client = ec2Client;
        this.replicaConfig = replicaConfig;
        this.replicaRepository = replicaRepository;
    }

    public Replica createReplica(String userId, String chronicleName, ReplicaType replicaType) {
        // TODO: iam instance profile, add tags
        final RunInstancesResponse response = ec2Client.runInstances(RunInstancesRequest.builder()
                .imageId(replicaConfig.amiId())
                .instanceType(replicaConfig.instanceType())
                .minCount(1)
                .maxCount(1)
                .networkInterfaces(InstanceNetworkInterfaceSpecification.builder()
                        .associatePublicIpAddress(true)
                        .subnetId(replicaConfig.subnetId())
                        .groups(replicaConfig.securityGroupId())
                        .deviceIndex(0)
                        .build())
                .build());

        final String ec2InstanceId = response.instances().get(0).instanceId();

        final Replica replica = new Replica(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                replicaType,
                ec2InstanceId,
                null,
                ReplicaStatus.PROVISIONING,
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
                .instanceIds(replica.ec2InstanceId())
                .build());

        replicaRepository.deleteById(replica.id());
    }
}
