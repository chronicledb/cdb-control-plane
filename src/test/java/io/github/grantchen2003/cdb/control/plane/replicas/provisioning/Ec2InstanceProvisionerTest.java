package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisApplierProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Ec2InstanceProvisionerTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaConfig replicaConfig;

    @Test
    void provision_sendsCorrectRunInstancesRequest() {
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn("t2.micro");
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.redisApplierSecurityGroupId()).thenReturn("sg-applier");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");

        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId("i-abc123").build())
                        .build());

        final Ec2InstanceProvisioner provisioner = new RedisApplierProvisioner(ec2Client, replicaConfig);
        provisioner.provision("cdb-replica_user-1_my-chronicle_applier");

        final ArgumentCaptor<RunInstancesRequest> captor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        verify(ec2Client).runInstances(captor.capture());

        final RunInstancesRequest request = captor.getValue();
        final InstanceNetworkInterfaceSpecification networkInterface = request.networkInterfaces().get(0);

        assertThat(request.imageId()).isEqualTo("ami-12345678");
        assertThat(request.instanceType().toString()).isEqualTo("t2.micro");
        assertThat(request.minCount()).isEqualTo(1);
        assertThat(request.maxCount()).isEqualTo(1);
        assertThat(networkInterface.subnetId()).isEqualTo("subnet-12345678");
        assertThat(networkInterface.groups()).containsExactly("sg-applier");
        assertThat(request.iamInstanceProfile().name()).isEqualTo("cdb-replica-profile");
        assertThat(request.tagSpecifications().get(0).tags().get(0).key()).isEqualTo("Name");
        assertThat(request.tagSpecifications().get(0).tags().get(0).value()).isEqualTo("cdb-replica_user-1_my-chronicle_applier");
    }

    @Test
    void provision_returnsInstanceId() {
        when(replicaConfig.amiId()).thenReturn("ami-12345678");
        when(replicaConfig.instanceType()).thenReturn("t2.micro");
        when(replicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(replicaConfig.redisApplierSecurityGroupId()).thenReturn("sg-applier");
        when(replicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");

        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId("i-abc123").build())
                        .build());

        final Ec2InstanceProvisioner provisioner = new RedisApplierProvisioner(ec2Client, replicaConfig);
        final String instanceId = provisioner.provision("cdb-replica_user-1_my-chronicle_applier");

        assertThat(instanceId).isEqualTo("i-abc123");
    }
}