package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaLifecycleManagerTest {

    @Mock
    private ReplicaRepository replicaRepository;

    @Mock
    private ReplicaConfig replicaConfig;

    @Mock
    private Ec2Client ec2Client;

    @InjectMocks
    private ReplicaLifecycleManager replicaLifecycleManager;

    private Replica newReplica() {
        return new Replica(
                "replica-1",
                "user-1",
                "my-chronicle",
                ReplicaType.REDIS,
                null,
                null,
                null,
                null,
                ReplicaStatus.NEW,
                Instant.now()
        );
    }

    private Replica provisioningReplica(final Instant createdAt) {
        return new Replica(
                "replica-1",
                "user-1",
                "my-chronicle",
                ReplicaType.REDIS,
                "i-applier-123",
                "i-storage-123",
                "i-txmanager-123",
                null,
                ReplicaStatus.PROVISIONING,
                createdAt
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

    // moveFromNewToProvisioning tests

    @Test
    void moveFromNewToProvisioning_launchesAllInstancesAndSavesProvisioningReplica() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn("t2.micro");
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.securityGroupId()).thenReturn("sg-12345678");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");

        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder().instances(Instance.builder().instanceId("i-applier-123").build()).build())
                .thenReturn(RunInstancesResponse.builder().instances(Instance.builder().instanceId("i-storage-123").build()).build())
                .thenReturn(RunInstancesResponse.builder().instances(Instance.builder().instanceId("i-txmanager-123").build()).build());

        replicaLifecycleManager.moveFromNewToProvisioning();

        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        final Replica saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(ReplicaStatus.PROVISIONING);
        assertThat(saved.applierInstanceId()).isNotNull();
        assertThat(saved.storageEngineInstanceId()).isNotNull();
        assertThat(saved.txManagerInstanceId()).isNotNull();
    }

    @Test
    void moveFromNewToProvisioning_marksErrorAndTerminatesLaunched_whenAnyLaunchFails() {
        final Replica replica = newReplica();
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of(replica));
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn("t2.micro");
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.securityGroupId()).thenReturn("sg-12345678");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");

        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder().instances(Instance.builder().instanceId("i-applier-123").build()).build())
                .thenThrow(new RuntimeException("EC2 error"));

        when(ec2Client.terminateInstances(any(TerminateInstancesRequest.class)))
                .thenReturn(TerminateInstancesResponse.builder().build());

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(ec2Client).terminateInstances(any(TerminateInstancesRequest.class));
        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.ERROR);
    }

    @Test
    void moveFromNewToProvisioning_doesNothing_whenNoNewReplicas() {
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(List.of());

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));
        verify(replicaRepository, never()).save(any());
    }

    // moveFromProvisioningToRunning tests

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
}