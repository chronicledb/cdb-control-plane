package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.ApplierProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.StorageEngineProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.TxManagerProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchema;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
public class ReplicaLifecycleManager {

    private static final Duration PROVISIONING_TIMEOUT = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(ReplicaLifecycleManager.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    private final ReplicaRepository replicaRepository;
    private final WriteSchemaService writeSchemaService;
    private final Ec2Client ec2Client;
    private final ApplierProvisionerFactory applierProvisionerFactory;
    private final StorageEngineProvisionerFactory storageEngineProvisionerFactory;
    private final TxManagerProvisionerFactory txManagerProvisionerFactory;

    public ReplicaLifecycleManager(
            ReplicaRepository replicaRepository,
            WriteSchemaService writeSchemaService,
            Ec2Client ec2Client,
            ApplierProvisionerFactory applierProvisionerFactory,
            StorageEngineProvisionerFactory storageEngineProvisionerFactory,
            TxManagerProvisionerFactory txManagerProvisionerFactory
    ) {
        this.replicaRepository = replicaRepository;
        this.writeSchemaService = writeSchemaService;
        this.ec2Client = ec2Client;
        this.applierProvisionerFactory = applierProvisionerFactory;
        this.storageEngineProvisionerFactory = storageEngineProvisionerFactory;
        this.txManagerProvisionerFactory = txManagerProvisionerFactory;
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

        final Optional<WriteSchema> writeSchemaOpt = writeSchemaService.findByUserIdAndChronicleName(replica.userId(), replica.chronicleName());
        if (writeSchemaOpt.isEmpty()) {
            throw new IllegalStateException(
                    "No write schema found for userId=" + replica.userId() + ", chronicleName=" + replica.chronicleName()
            );
        }

        final Ec2InstanceProvisioner storageEngineProvisioner = storageEngineProvisionerFactory.forType(replica.type());

        String storageEngineInstanceId = null;
        final String storageEngineHost;

        try {
            storageEngineInstanceId = storageEngineProvisioner.provision(namePrefix + "_storage-engine");
            storageEngineHost = waitForPublicIp(storageEngineInstanceId);
        } catch (Exception e) {
            log.error("Failed to provision storage engine for replica={}: {}", replica.id(), e.getMessage(), e);

            if (storageEngineInstanceId != null) {
                ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                        .instanceIds(storageEngineInstanceId)
                        .build());
            }
            replicaRepository.save(replica.withStatus(ReplicaStatus.ERROR));
            return;
        }

        final Ec2InstanceProvisioner applierProvisioner = applierProvisionerFactory.forType(
                replica.type(), replica.chronicleId(), writeSchemaOpt.get().writeSchemaJson(), storageEngineHost);
        final Ec2InstanceProvisioner txManagerProvisioner = txManagerProvisionerFactory.forType(
                replica.type(), replica.chronicleId(), writeSchemaOpt.get().writeSchemaJson());

        final CompletableFuture<String> applierFuture = CompletableFuture.supplyAsync(
                () -> applierProvisioner.provision(namePrefix + "_applier"), executor);
        final CompletableFuture<String> txManagerFuture = CompletableFuture.supplyAsync(
                () -> txManagerProvisioner.provision(namePrefix + "_tx-manager"), executor);

        try {
            final String applierInstanceId = applierFuture.join();
            final String txManagerInstanceId = txManagerFuture.join();

            replicaRepository.save(replica
                    .withApplierInstanceId(applierInstanceId)
                    .withStorageEngineInstanceId(storageEngineInstanceId)
                    .withTxManagerInstanceId(txManagerInstanceId)
                    .withStatus(ReplicaStatus.PROVISIONING));
        } catch (Exception e) {
            final List<String> launched = Stream.of(applierFuture, txManagerFuture)
                    .filter(f -> f.isDone() && !f.isCompletedExceptionally())
                    .map(CompletableFuture::join)
                    .toList();

            final List<String> toTerminate = Stream.concat(
                    Stream.of(storageEngineInstanceId),
                    launched.stream()
            ).toList();

            ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(toTerminate)
                    .build());

            replicaRepository.save(replica.withStatus(ReplicaStatus.ERROR));

            log.error("Failed to provision applier/tx-manager for replica={}, terminating {} instance(s): {}",
                    replica.id(), toTerminate.size(), e.getMessage(), e);
        }
    }

    private String waitForPublicIp(String instanceId) {
        final Instant deadline = Instant.now().plus(PROVISIONING_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            final Instance instance = describeInstance(instanceId);
            if (instance.state().name().equals(InstanceStateName.RUNNING) && instance.publicIpAddress() != null) {
                return instance.publicIpAddress();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for storage engine IP", e);
            }
        }
        throw new RuntimeException("Timed out waiting for storage engine public IP: " + instanceId);
    }

    private void moveReplicaFromProvisioningToRunning(Replica replica) {
        final Instance applier = describeInstance(replica.applierInstanceId());
        final Instance storageEngine = describeInstance(replica.storageEngineInstanceId());
        final Instance txManager = describeInstance(replica.txManagerInstanceId());

        final List<Instance> allInstances = List.of(applier, storageEngine, txManager);

        final boolean anyExceededTimeout = hasExceededProvisioningTimeout(replica) &&
                allInstances.stream().anyMatch(i -> i.state().name().equals(InstanceStateName.PENDING));

        if (anyExceededTimeout) {
            log.error("Replica={} exceeded provisioning timeout of {}min with pending instance(s), terminating all",
                    replica.id(), PROVISIONING_TIMEOUT.toMinutes());
            ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(
                            replica.applierInstanceId(),
                            replica.storageEngineInstanceId(),
                            replica.txManagerInstanceId()
                    )
                    .build());
            replicaRepository.save(replica.withStatus(ReplicaStatus.ERROR));
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