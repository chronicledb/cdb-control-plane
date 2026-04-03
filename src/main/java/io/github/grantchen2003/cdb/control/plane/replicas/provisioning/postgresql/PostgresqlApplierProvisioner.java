package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql;

import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class PostgresqlApplierProvisioner extends Ec2InstanceProvisioner {

    public PostgresqlApplierProvisioner(Ec2Client ec2Client, PostgresqlReplicaConfig postgresqlReplicaConfig) {
        super(ec2Client, postgresqlReplicaConfig);
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.applierSecurityGroupId();
    }
}