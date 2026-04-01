package io.github.grantchen2003.cdb.control.plane.replicas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class ReplicaStatusSynchronizer {

    private static final Duration PROVISIONING_TIMEOUT = Duration.ofMinutes(10);

    private final ReplicaRepository replicaRepository;
    private final Ec2Client ec2Client;

    public ReplicaStatusSynchronizer(ReplicaRepository replicaRepository, Ec2Client ec2Client) {
        this.replicaRepository = replicaRepository;
        this.ec2Client = ec2Client;
    }

    @Scheduled(fixedDelay = 60000)
    public void sync() {
        replicaRepository.findByStatus(ReplicaStatus.PROVISIONING).forEach(this::syncReplica);
    }

    private void syncReplica(Replica replica) {
        final Instance applier = describeInstance(replica.applierInstanceId());
        final Instance storageEngine = describeInstance(replica.storageEngineInstanceId());
        final Instance txManager = describeInstance(replica.txManagerInstanceId());

        final List<Instance> allInstances = List.of(applier, storageEngine, txManager);

        final boolean anyExceededTimeout = hasExceededProvisioningTimeout(replica) &&
                allInstances.stream().anyMatch(i -> i.state().name().equals(InstanceStateName.PENDING));

        if (anyExceededTimeout) {
            ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(
                            replica.applierInstanceId(),
                            replica.storageEngineInstanceId(),
                            replica.txManagerInstanceId()
                    )
                    .build());
            replicaRepository.save(replica.withStatus(ReplicaStatus.FAILED_PROVISION));
            return;
        }

        final boolean allRunning = allInstances.stream()
                .allMatch(i -> i.state().name().equals(InstanceStateName.RUNNING));

        if (allRunning && txManager.publicIpAddress() != null) {
            replicaRepository.save(replica
                    .withStatus(ReplicaStatus.RUNNING)
                    .withTxManagerPublicIp(txManager.publicIpAddress()));
        }
    }

    private Instance describeInstance(String instanceId) {
        return ec2Client.describeInstances(DescribeInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build())
                .reservations().get(0).instances().get(0);
    }

    private boolean hasExceededProvisioningTimeout(Replica replica) {
        return Duration.between(replica.createdAt(), Instant.now()).compareTo(PROVISIONING_TIMEOUT) > 0;
    }
}