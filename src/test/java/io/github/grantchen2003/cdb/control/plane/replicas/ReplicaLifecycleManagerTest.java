package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.ApplierProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.StorageEngineProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.TxManagerProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchema;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaLifecycleManagerTest {

    private static final String STORAGE_ENGINE_HOST = "1.2.3.4";

    @Mock private ReplicaRepository replicaRepository;
    @Mock private WriteSchemaService writeSchemaService;
    @Mock private Ec2Client ec2Client;
    @Mock private ApplierProvisionerFactory applierProvisionerFactory;
    @Mock private StorageEngineProvisionerFactory storageEngineProvisionerFactory;
    @Mock private TxManagerProvisionerFactory txManagerProvisionerFactory;

    @InjectMocks
    private ReplicaLifecycleManager replicaLifecycleManager;

    private Replica newReplica() {
        return new Replica(
                "replica-1", "user-1", "chronicle-1", "my-chronicle",
                ReplicaType.REDIS, null, null, null, null,
                ReplicaStatus.NEW, Instant.now()
        );
    }

    private Replica provisioningReplica(final Instant createdAt) {
        return new Replica(
                "replica-1", "user-1", "chronicle-1", "my-chronicle",
                ReplicaType.REDIS, "i-applier-123", "i-storage-123", "i-txmanager-123", null,
                ReplicaStatus.PROVISIONING, createdAt
        );
    }

    private DescribeInstancesResponse instanceResponse(final InstanceStateName state, final String publicIp) {
        final Instance instance = Instance.builder()
                .state(InstanceState.builder().name(state).build())
                .publicIpAddress(publicIp)
                .build();
        return DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();
    }

    private void mockWriteSchema() {
        final WriteSchema writeSchema = new WriteSchema(
                "ws-1", "user-1", "my-chronicle", "{}", Instant.now());
        when(writeSchemaService.findByUserIdAndChronicleName("user-1", "my-chronicle"))
                .thenReturn(Optional.of(writeSchema));
    }

    // Mocks storage engine provision + waitForPublicIp, then applier + txManager with the resolved host
    private void mockProvisioners(String applierInstanceId, String storageEngineInstanceId, String txManagerInstanceId) {
        final Ec2InstanceProvisioner applierProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner storageEngineProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner txManagerProvisioner = mock(Ec2InstanceProvisioner.class);

        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(applierProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(applierProvisioner);
        when(txManagerProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(txManagerProvisioner);

        when(storageEngineProvisioner.provision(anyString())).thenReturn(storageEngineInstanceId);
        when(applierProvisioner.provision(anyString())).thenReturn(applierInstanceId);
        when(txManagerProvisioner.provision(anyString())).thenReturn(txManagerInstanceId);

        // waitForPublicIp polls describeInstances until RUNNING + public IP
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, STORAGE_ENGINE_HOST));
    }

    // ── moveFromNewToProvisioning ─────────────────────────────────────────────

    @Test
    void moveFromNewToProvisioning_provisionsAllInstancesAndSavesProvisioningReplica() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        mockWriteSchema();
        mockProvisioners("i-applier-123", "i-storage-123", "i-txmanager-123");

        replicaLifecycleManager.moveFromNewToProvisioning();

        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        final Replica saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(ReplicaStatus.PROVISIONING);
        assertThat(saved.applierInstanceId()).isEqualTo("i-applier-123");
        assertThat(saved.storageEngineInstanceId()).isEqualTo("i-storage-123");
        assertThat(saved.txManagerInstanceId()).isEqualTo("i-txmanager-123");
    }

    @Test
    void moveFromNewToProvisioning_marksErrorWithoutTerminating_whenStorageEngineProvisionFails() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        mockWriteSchema();

        final Ec2InstanceProvisioner storageEngineProvisioner = mock(Ec2InstanceProvisioner.class);
        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(storageEngineProvisioner.provision(anyString())).thenThrow(new RuntimeException("EC2 error"));

        replicaLifecycleManager.moveFromNewToProvisioning();

        // nothing was successfully launched so no termination
        verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.ERROR);
    }

    @Test
    void moveFromNewToProvisioning_terminatesStorageEngineAndMarksError_whenProvisionSucceedsButWaitForPublicIpFails() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        mockWriteSchema();

        final Ec2InstanceProvisioner storageEngineProvisioner = mock(Ec2InstanceProvisioner.class);
        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(storageEngineProvisioner.provision(anyString())).thenReturn("i-storage-123");

        // waitForPublicIp polls describeInstances — make it blow up
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(new RuntimeException("EC2 describe failed"));
        when(ec2Client.terminateInstances(any(TerminateInstancesRequest.class)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        replicaLifecycleManager.moveFromNewToProvisioning();

        // The storage engine was launched before the failure, so it must be cleaned up
        final ArgumentCaptor<TerminateInstancesRequest> terminateCaptor =
                ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(ec2Client).terminateInstances(terminateCaptor.capture());
        assertThat(terminateCaptor.getValue().instanceIds()).containsExactly("i-storage-123");

        final ArgumentCaptor<Replica> replicaCaptor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(replicaCaptor.capture());
        assertThat(replicaCaptor.getValue().status()).isEqualTo(ReplicaStatus.ERROR);
    }

    @Test
    void moveFromNewToProvisioning_terminatesStorageEngineAndMarksError_whenApplierOrTxManagerProvisionFails() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        mockWriteSchema();

        final Ec2InstanceProvisioner storageEngineProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner applierProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner txManagerProvisioner = mock(Ec2InstanceProvisioner.class);

        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(applierProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(applierProvisioner);
        when(txManagerProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(txManagerProvisioner);

        when(storageEngineProvisioner.provision(anyString())).thenReturn("i-storage-123");
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, STORAGE_ENGINE_HOST));
        when(applierProvisioner.provision(anyString())).thenThrow(new RuntimeException("EC2 error"));
        when(txManagerProvisioner.provision(anyString())).thenReturn("i-txmanager-123");
        when(ec2Client.terminateInstances(any(TerminateInstancesRequest.class)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        replicaLifecycleManager.moveFromNewToProvisioning();

        final ArgumentCaptor<TerminateInstancesRequest> terminateCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(ec2Client).terminateInstances(terminateCaptor.capture());
        assertThat(terminateCaptor.getValue().instanceIds()).contains("i-storage-123");

        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.ERROR);
    }

    @Test
    void moveFromNewToProvisioning_doesNothing_whenNoNewReplicas() {
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of());

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(storageEngineProvisionerFactory, never()).forType(any());
        verify(applierProvisionerFactory, never()).forType(any(), any(), any(), any());
        verify(txManagerProvisionerFactory, never()).forType(any(), any(), any(), any());
        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromNewToProvisioning_marksError_whenWriteSchemaNotFound() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        when(writeSchemaService.findByUserIdAndChronicleName("user-1", "my-chronicle"))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> replicaLifecycleManager.moveFromNewToProvisioning());
    }

    // ── moveFromProvisioningToRunning ─────────────────────────────────────────

    @Test
    void moveFromProvisioningToRunning_promotesToRunning_whenAllInstancesRunningWithTxManagerPublicIp() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, "203.0.113.10"));

        replicaLifecycleManager.moveFromProvisioningToRunning();

        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.RUNNING);
        assertThat(captor.getValue().txManagerPublicIp()).isEqualTo("203.0.113.10");
    }

    @Test
    void moveFromProvisioningToRunning_doesNothing_whenAllRunningButTxManagerPublicIpIsNull() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null));

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromProvisioningToRunning_doesNothing_whenNotAllInstancesRunning() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.PENDING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, "203.0.113.10"));

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromProvisioningToRunning_terminatesAllAndMarksError_whenAnyInstancePendingAndTimedOut() {
        final Replica replica = provisioningReplica(Instant.now().minus(Duration.ofMinutes(11)));
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, null))
                .thenReturn(instanceResponse(InstanceStateName.PENDING, null))
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, "203.0.113.10"));
        when(ec2Client.terminateInstances(any(TerminateInstancesRequest.class)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        replicaLifecycleManager.moveFromProvisioningToRunning();

        final ArgumentCaptor<TerminateInstancesRequest> terminateCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(ec2Client).terminateInstances(terminateCaptor.capture());
        assertThat(terminateCaptor.getValue().instanceIds()).containsExactlyInAnyOrder(
                replica.applierInstanceId(),
                replica.storageEngineInstanceId(),
                replica.txManagerInstanceId()
        );

        final ArgumentCaptor<Replica> replicaCaptor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(replicaCaptor.capture());
        assertThat(replicaCaptor.getValue().status()).isEqualTo(ReplicaStatus.ERROR);
    }

    @Test
    void moveFromProvisioningToRunning_doesNothing_whenPendingAndNotTimedOut() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(instanceResponse(InstanceStateName.PENDING, null))
                .thenReturn(instanceResponse(InstanceStateName.PENDING, null))
                .thenReturn(instanceResponse(InstanceStateName.PENDING, null));

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromProvisioningToRunning_doesNothing_whenNoProvisioningReplicas() {
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of());

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(ec2Client, never()).describeInstances(any(DescribeInstancesRequest.class));
        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromNewToProvisioning_retriesAndSucceeds_whenDescribeInstancesInitiallyReturnsNotFound() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        mockWriteSchema();

        final Ec2InstanceProvisioner storageEngineProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner applierProvisioner = mock(Ec2InstanceProvisioner.class);
        final Ec2InstanceProvisioner txManagerProvisioner = mock(Ec2InstanceProvisioner.class);

        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(applierProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(applierProvisioner);
        when(txManagerProvisionerFactory.forType(ReplicaType.REDIS, "chronicle-1", "{}", STORAGE_ENGINE_HOST)).thenReturn(txManagerProvisioner);

        when(storageEngineProvisioner.provision(anyString())).thenReturn("i-storage-123");
        when(applierProvisioner.provision(anyString())).thenReturn("i-applier-123");
        when(txManagerProvisioner.provision(anyString())).thenReturn("i-txmanager-123");

        final Ec2Exception notFoundException = (Ec2Exception) Ec2Exception.builder()
                .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                        .errorCode("InvalidInstanceID.NotFound")
                        .errorMessage("The instance ID does not exist")
                        .build())
                .build();

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(notFoundException)
                .thenReturn(instanceResponse(InstanceStateName.RUNNING, STORAGE_ENGINE_HOST));

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.PROVISIONING);
    }
}