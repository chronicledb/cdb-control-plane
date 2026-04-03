package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisApplierProvisionerTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaConfig replicaConfig;

    @Test
    void provision_usesRedisApplierSecurityGroup() {
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn("t2.micro");
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.redisApplierSecurityGroupId()).thenReturn("sg-applier");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");
        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId("i-abc123").build())
                        .build());

        new RedisApplierProvisioner(ec2Client, replicaConfig).provision("name");

        assertThat(replicaConfig.redisApplierSecurityGroupId()).isEqualTo("sg-applier");
    }
}