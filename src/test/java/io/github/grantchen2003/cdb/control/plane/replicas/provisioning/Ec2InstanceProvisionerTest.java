package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql.PostgresqlApplierProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisApplierProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Ec2InstanceProvisionerTest {

    @Mock
    private Ec2Client ec2Client;

    @Test
    void provision_withRedisConfig_sendsCorrectRequest() {
        final RedisReplicaConfig redisReplicaConfig = new RedisReplicaConfig(
                "ami-redis",
                "t3.micro",
                "subnet-123",
                "sg-tx",
                "sg-engine",
                "sg-redis-applier",
                "cdb-profile"
        );

        stubEc2RunInstances("i-redis");

        final Ec2InstanceProvisioner provisioner = new RedisApplierProvisioner(ec2Client, redisReplicaConfig);
        provisioner.provision("redis-instance");

        final RunInstancesRequest request = captureRequest();
        assertThat(request.imageId()).isEqualTo("ami-redis");
        assertThat(request.networkInterfaces().get(0).groups()).containsExactly("sg-redis-applier");
    }

    @Test
    void provision_withPostgresqlConfig_sendsCorrectRequest() {
        final PostgresqlReplicaConfig postgresqlReplicaConfig = new PostgresqlReplicaConfig(
                "ami-pg",
                "t3.medium",
                "subnet-456",
                "sg-pg-tx",
                "sg-pg-engine",
                "sg-pg-applier",
                "cdb-profile"
        );

        stubEc2RunInstances("i-pg");

        final Ec2InstanceProvisioner provisioner = new PostgresqlApplierProvisioner(ec2Client, postgresqlReplicaConfig);
        provisioner.provision("pg-instance");

        final RunInstancesRequest request = captureRequest();
        assertThat(request.imageId()).isEqualTo("ami-pg");
        assertThat(request.instanceType()).isEqualTo(InstanceType.T3_MEDIUM);
        assertThat(request.networkInterfaces().get(0).groups()).containsExactly("sg-pg-applier");
    }

    @Test
    void provision_handlesTagsCorrectly() {
        final RedisReplicaConfig config = new RedisReplicaConfig(
                "ami-123", "t3.micro", "sub-1", "tx", "eng", "app", "prof"
        );
        stubEc2RunInstances("i-123");

        new RedisApplierProvisioner(ec2Client, config).provision("test-tag-name");

        final RunInstancesRequest request = captureRequest();
        final Tag nameTag = request.tagSpecifications().get(0).tags().get(0);

        assertThat(nameTag.key()).isEqualTo("Name");
        assertThat(nameTag.value()).isEqualTo("test-tag-name");
    }

    private void stubEc2RunInstances(String instanceId) {
        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId(instanceId).build())
                        .build());
    }

    private RunInstancesRequest captureRequest() {
        final ArgumentCaptor<RunInstancesRequest> captor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        verify(ec2Client).runInstances(captor.capture());
        return captor.getValue();
    }
}