package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
public class ReplicaLifecycleManager {

    private static final Duration PROVISIONING_TIMEOUT = Duration.ofMinutes(10);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final ReplicaRepository replicaRepository;
    private final ReplicaConfig replicaConfig;
    private final Ec2Client ec2Client;

    public ReplicaLifecycleManager(ReplicaRepository replicaRepository, ReplicaConfig replicaConfig, Ec2Client ec2Client) {
        this.replicaRepository = replicaRepository;
        this.replicaConfig = replicaConfig;
        this.ec2Client = ec2Client;
    }

    @Scheduled(fixedDelay = 10000)
    public void moveFromNewToProvisioning() {
        replicaRepository.findByStatus(ReplicaStatus.NEW).forEach(this::moveReplicaFromNewToProvisioning);
    }

    @Scheduled(fixedDelay = 60000)
    public void moveFromProvisioningToRunning() {
        replicaRepository.findByStatus(ReplicaStatus.PROVISIONING).forEach(this::moveReplicaFromProvisioningToRunning);
    }

    private void moveReplicaFromNewToProvisioning(Replica replica) {
        final String namePrefix = "cdb-replica_" + replica.userId() + "_" + replica.chronicleName();

        final CompletableFuture<String> applierFuture = CompletableFuture.supplyAsync(
                () -> launchInstance(replicaConfig.securityGroupId(), false, namePrefix + "_applier"), executor);
        final CompletableFuture<String> storageEngineFuture = CompletableFuture.supplyAsync(
                () -> launchInstance(replicaConfig.securityGroupId(), false, namePrefix + "_storage-engine"), executor);
        final CompletableFuture<String> txManagerFuture = CompletableFuture.supplyAsync(
                () -> launchInstance(replicaConfig.securityGroupId(), true, namePrefix + "_tx-manager"), executor);

        try {
            final String applierInstanceId       = applierFuture.join();
            final String storageEngineInstanceId = storageEngineFuture.join();
            final String txManagerInstanceId     = txManagerFuture.join();

            replicaRepository.save(replica
                    .withApplierInstanceId(applierInstanceId)
                    .withStorageEngineInstanceId(storageEngineInstanceId)
                    .withTxManagerInstanceId(txManagerInstanceId)
                    .withStatus(ReplicaStatus.PROVISIONING));
        } catch (Exception e) {
            final List<String> launched = Stream.of(applierFuture, storageEngineFuture, txManagerFuture)
                    .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                    .map(CompletableFuture::join)
                    .toList();

            if (!launched.isEmpty()) {
                ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                        .instanceIds(launched)
                        .build());
            }

            replicaRepository.save(replica.withStatus(ReplicaStatus.ERROR));
        }
    }

    private void moveReplicaFromProvisioningToRunning(Replica replica) {
        final Instance applier       = describeInstance(replica.applierInstanceId());
        final Instance storageEngine = describeInstance(replica.storageEngineInstanceId());
        final Instance txManager     = describeInstance(replica.txManagerInstanceId());

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
            replicaRepository.save(replica.withStatus(ReplicaStatus.ERROR ));
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

    private String launchInstance(String securityGroupId, boolean associatePublicIp, String name) {
        return ec2Client.runInstances(RunInstancesRequest.builder()
                        .imageId(replicaConfig.amiId())
                        .instanceType(replicaConfig.instanceType())
                        .minCount(1)
                        .maxCount(1)
                        .networkInterfaces(InstanceNetworkInterfaceSpecification.builder()
                                .associatePublicIpAddress(associatePublicIp)
                                .subnetId(replicaConfig.subnetId())
                                .groups(securityGroupId)
                                .deviceIndex(0)
                                .build())
                        .iamInstanceProfile(IamInstanceProfileSpecification.builder()
                                .name(replicaConfig.iamInstanceProfileName())
                                .build())
                        .tagSpecifications(TagSpecification.builder()
                                .resourceType(ResourceType.INSTANCE)
                                .tags(Tag.builder()
                                        .key("Name")
                                        .value(name)
                                        .build())
                                .build())
                        .build())
                .instances().get(0).instanceId();
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