package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.chronicles.Chronicle;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaServiceTest {

    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_ID   = "chronicle-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String REPLICA_TYPE   = "REDIS";
    private static final Replica REPLICA = new Replica(
            "replica-id", USER_ID, CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
            "i-applier-123", "i-storage-123", "i-txmanager-123",
            null, ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
    );
    private static final Chronicle CHRONICLE = new Chronicle(CHRONICLE_ID, USER_ID, CHRONICLE_NAME, "write-schema-123", Instant.parse("2024-01-01T00:00:00Z"));

    @Mock
    private ChronicleService chronicleService;

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaRepository replicaRepository;

    @InjectMocks
    private ReplicaService replicaService;

    @Test
    void createReplica_savesNewReplicaWithoutLaunchingInstances() {
        when(chronicleService.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.of(CHRONICLE));

        final Replica result = replicaService.createReplica(USER_ID, CHRONICLE_NAME, REPLICA_TYPE);

        assertThat(result.id()).isNotNull();
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.chronicleId()).isEqualTo(CHRONICLE_ID);
        assertThat(result.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(result.type()).isEqualTo(ReplicaType.REDIS);
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
    void createReplica_chronicleNotFound_throwsChronicleNotFoundException() {
        when(chronicleService.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replicaService.createReplica(USER_ID, CHRONICLE_NAME, REPLICA_TYPE))
                .isInstanceOf(ChronicleNotFoundException.class);
    }

    @Test
    void createReplica_invalidReplicaType_throwsInvalidReplicaTypeException() {
        when(chronicleService.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.of(CHRONICLE));

        assertThatThrownBy(() -> replicaService.createReplica(USER_ID, CHRONICLE_NAME, "INVALID_TYPE"))
                .isInstanceOf(InvalidReplicaTypeException.class)
                .hasMessageContaining("INVALID_TYPE");
    }

    @Test
    void delete_success() {
        replicaService.delete(REPLICA);

        verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(
                        REPLICA.applierInstanceId(),
                        REPLICA.storageEngineInstanceId(),
                        REPLICA.txManagerInstanceId()
                )
                .build());
        verify(replicaRepository).deleteById(REPLICA.id());
    }

    @Test
    void findById_found_returnsReplica() {
        when(replicaRepository.findById(REPLICA.id())).thenReturn(Optional.of(REPLICA));

        assertThat(replicaService.findById(REPLICA.id())).contains(REPLICA);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(replicaRepository.findById(REPLICA.id())).thenReturn(Optional.empty());

        assertThat(replicaService.findById(REPLICA.id())).isEmpty();
    }
}