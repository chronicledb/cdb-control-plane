package io.github.grantchen2003.cdb.control.plane.replicas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReplicaServiceTest {
    private static final String userId        = "3e30e447-ecd4-48b0-b592-207cd16b0609";
    private static final String chronicleName = "my-chronicle";
    private static final ReplicaType type     = ReplicaType.REDIS;

    @Mock
    private ReplicaRepository replicaRepository;

    @InjectMocks
    private ReplicaService replicaService;

    @Test
    void createReplica_success() {
        final Replica replica = replicaService.createReplica(userId, chronicleName, type);

        assertThat(replica.id()).isNotNull();
        assertThat(replica.userId()).isEqualTo(userId);
        assertThat(replica.chronicleName()).isEqualTo(chronicleName);
        assertThat(replica.type()).isEqualTo(type);
        assertThat(replica.ec2InstanceId()).isNotNull();
        assertThat(replica.createdAt()).isNotNull();
        verify(replicaRepository).save(any(Replica.class));
    }
}