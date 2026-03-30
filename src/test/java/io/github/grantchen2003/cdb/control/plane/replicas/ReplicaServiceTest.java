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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaServiceTest {
    private static final String userId        = "3e30e447-ecd4-48b0-b592-207cd16b0609";
    private static final String chronicleName = "my-chronicle";
    private static final ReplicaType type     = ReplicaType.REDIS;

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

        final Instance mockInstance = Instance.builder().instanceId("i-abc123").build();
        final RunInstancesResponse mockResponse = RunInstancesResponse.builder()
                .instances(mockInstance)
                .build();

        when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(mockResponse);


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