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
        final Instance instance = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                        .instanceIds(replica.ec2InstanceId())
                        .build())
                .reservations().get(0).instances().get(0);

        final InstanceStateName state = instance.state().name();

        if (state.equals(InstanceStateName.RUNNING) && instance.publicIpAddress() != null) {
            replicaRepository.save(replica.withStatus(ReplicaStatus.RUNNING).withPublicIp(instance.publicIpAddress()));
        } else if (state.equals(InstanceStateName.PENDING) && hasExceededProvisioningTimeout(replica)) {
            ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(replica.ec2InstanceId())
                    .build());
            replicaRepository.save(replica.withStatus(ReplicaStatus.FAILED_PROVISION));
        }
    }

    private boolean hasExceededProvisioningTimeout (Replica replica) {
        return Duration.between(replica.createdAt(), Instant.now()).compareTo(PROVISIONING_TIMEOUT) > 0;
    }
}
