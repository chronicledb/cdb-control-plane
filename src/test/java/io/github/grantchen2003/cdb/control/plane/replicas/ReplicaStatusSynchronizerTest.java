package io.github.grantchen2003.cdb.control.plane.replicas;

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
class ReplicaStatusSynchronizerTest {

    @Mock
    private ReplicaRepository replicaRepository;

    @Mock
    private Ec2Client ec2Client;

    @InjectMocks
    private ReplicaStatusSynchronizer synchronizer;

    private Replica provisioningReplica(final Instant createdAt) {
        return new Replica(
                "replica-1",
                "user-1",
                "my-chronicle",
                ReplicaType.REDIS,
                "i-0abc123",
                null,
                ReplicaStatus.PROVISIONING,
                createdAt
        );
    }

    private void mockEc2State(final InstanceStateName state, final String publicIp) {
        final Instance instance = Instance.builder()
                .state(InstanceState.builder().name(state).build())
                .publicIpAddress(publicIp)
                .build();

        final DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance).build())
                .build();

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(response);
    }

    @Test
    void sync_promotesToRunning_whenInstanceIsRunningWithPublicIp() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));
        mockEc2State(InstanceStateName.RUNNING, "203.0.113.10");

        synchronizer.sync();

        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.RUNNING);
        assertThat(captor.getValue().publicIp()).isEqualTo("203.0.113.10");
    }

    @Test
    void sync_doesNothing_whenInstanceIsRunningButPublicIpIsNull() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));
        mockEc2State(InstanceStateName.RUNNING, null);

        synchronizer.sync();

        verify(replicaRepository, never()).save(any());
    }

    @Test
    void sync_terminatesAndMarksFailedProvision_whenPendingAndTimedOut() {
        final Replica replica = provisioningReplica(Instant.now().minus(Duration.ofMinutes(11)));
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));
        mockEc2State(InstanceStateName.PENDING, null);
        when(ec2Client.terminateInstances(any(TerminateInstancesRequest.class))).thenReturn(TerminateInstancesResponse.builder().build());

        synchronizer.sync();

        verify(ec2Client).terminateInstances(any(TerminateInstancesRequest.class));
        final ArgumentCaptor<Replica> captor = ArgumentCaptor.forClass(Replica.class);
        verify(replicaRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(ReplicaStatus.FAILED_PROVISION);
    }

    @Test
    void sync_doesNothing_whenPendingAndNotTimedOut() {
        final Replica replica = provisioningReplica(Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of(replica));
        mockEc2State(InstanceStateName.PENDING, null);

        synchronizer.sync();

        verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
        verify(replicaRepository, never()).save(any());
    }

    @Test
    void sync_doesNothing_whenNoProvisioningReplicas() {
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(List.of());

        synchronizer.sync();

        verify(ec2Client, never()).describeInstances(any(DescribeInstancesRequest.class));
        verify(replicaRepository, never()).save(any());
    }
}