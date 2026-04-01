package io.github.grantchen2003.cdb.control.plane.replicas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
            "i-applier-123",
            "i-storage-123",
            "i-txmanager-123",
            null,
            ReplicaStatus.PROVISIONING,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaRepository replicaRepository;

    @InjectMocks
    private ReplicaService replicaService;

    @Test
    void createReplica_savesNewReplicaWithoutLaunchingInstances() {
        final Replica result = replicaService.createReplica(userId, chronicleName, type);

        assertThat(result.id()).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.chronicleName()).isEqualTo(chronicleName);
        assertThat(result.type()).isEqualTo(type);
        assertThat(result.applierInstanceId()).isNull();
        assertThat(result.storageEngineInstanceId()).isNull();
        assertThat(result.txManagerInstanceId()).isNull();
        assertThat(result.txManagerPublicIp()).isNull();
        assertThat(result.status()).isEqualTo(ReplicaStatus.NEW);
        assertThat(result.createdAt()).isNotNull();

        verify(replicaRepository).save(any(Replica.class));
        verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));
    }

    @Test
    void delete_success() {
        replicaService.delete(replica);

        verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(
                        replica.applierInstanceId(),
                        replica.storageEngineInstanceId(),
                        replica.txManagerInstanceId()
                )
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