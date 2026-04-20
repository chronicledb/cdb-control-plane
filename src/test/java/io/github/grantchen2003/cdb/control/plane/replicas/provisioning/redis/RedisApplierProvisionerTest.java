package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
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
    private AwsConfig awsConfig;

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private RedisReplicaConfig redisReplicaConfig;

    @Test
    void provision_usesRedisApplierSecurityGroup() {
        when(redisReplicaConfig.amiId()).thenReturn("ami-12345678");
        when(redisReplicaConfig.instanceType()).thenReturn("t2.micro");
        when(redisReplicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(redisReplicaConfig.applierSecurityGroupId()).thenReturn("sg-applier");
        when(redisReplicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");
        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId("i-abc123").build())
                        .build());

        new RedisApplierProvisioner(
                awsConfig,
                ec2Client,
                redisReplicaConfig,
                "localhost:9092",
                "chronicle-1",
                "{}",
                "localhost"
        ).provision("name");

        assertThat(redisReplicaConfig.applierSecurityGroupId()).isEqualTo("sg-applier");
    }
}