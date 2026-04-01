package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaServiceTest {
    private static final String userId        = "3e30e447-ecd4-48b0-b592-207cd16b0609";
    private static final String chronicleName = "my-chronicle";
    private static final ReplicaType type     = ReplicaType.REDIS;
    private static final Replica replica      = new Replica(
            "replica-id",
            userId,
            chronicleName,
            ReplicaType.REDIS,
            "i-0abc123def456",
            "203.0.113.10",
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaConfig replicaConfig;

    @Mock
    private ReplicaRepository replicaRepository;

    @InjectMocks
    private ReplicaService replicaService;

    @Test
    void createReplica_success() {
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn(InstanceType.T2_MICRO.toString());
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.securityGroupId()).thenReturn("sg-12345678");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");

        final Instance mockInstance = Instance.builder()
                .instanceId("i-abc123")
                .build();

        final RunInstancesResponse mockResponse = RunInstancesResponse.builder()
                .instances(mockInstance)
                .build();

        when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(mockResponse);;

        final Replica result = replicaService.createReplica(userId, chronicleName, type);

        assertThat(result.id()).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.chronicleName()).isEqualTo(chronicleName);
        assertThat(result.type()).isEqualTo(type);
        assertThat(result.ec2InstanceId()).isNotNull();
        assertThat(result.publicIp()).isNull();
        assertThat(result.status()).isEqualTo(ReplicaStatus.PROVISIONING);
        assertThat(result.createdAt()).isNotNull();
        verify(replicaRepository).save(any(Replica.class));
    }

    @Test
    void delete_success() {
        replicaService.delete(replica);

        verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(replica.ec2InstanceId())
                .build());

        verify(replicaRepository).deleteById(replica.id());
    }

    @Test
    void findById_found_returnsReplica() {
        when(replicaRepository.findById(replica.id())).thenReturn(Optional.of(replica));

        final Optional<Replica> result = replicaService.findById(replica.id());

        assertThat(result).contains(replica);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(replicaRepository.findById(replica.id())).thenReturn(Optional.empty());

        final Optional<Replica> result = replicaService.findById(replica.id());

        assertThat(result).isEmpty();
    }
}