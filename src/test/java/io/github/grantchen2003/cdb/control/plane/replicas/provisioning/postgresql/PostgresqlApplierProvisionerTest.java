package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql;

import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
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
class PostgresqlApplierProvisionerTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private PostgresqlReplicaConfig postgresqlReplicaConfig;

    @Test
    void provision_usesPostgresqlApplierSecurityGroup() {
        when(postgresqlReplicaConfig.amiId()).thenReturn("ami-12345678");
        when(postgresqlReplicaConfig.instanceType()).thenReturn("t2.micro");
        when(postgresqlReplicaConfig.subnetId()).thenReturn("subnet-12345678");
        when(postgresqlReplicaConfig.applierSecurityGroupId()).thenReturn("sg-pg-applier");
        when(postgresqlReplicaConfig.iamInstanceProfileName()).thenReturn("cdb-replica-profile");
        when(ec2Client.runInstances(any(RunInstancesRequest.class)))
                .thenReturn(RunInstancesResponse.builder()
                        .instances(Instance.builder().instanceId("i-abc123").build())
                        .build());

        new PostgresqlApplierProvisioner(ec2Client, postgresqlReplicaConfig).provision("name");

        assertThat(postgresqlReplicaConfig.applierSecurityGroupId()).isEqualTo("sg-pg-applier");
    }
}