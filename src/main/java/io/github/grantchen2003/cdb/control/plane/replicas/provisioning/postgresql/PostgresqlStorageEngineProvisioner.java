package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql;

import io.github.grantchen2003.cdb.control.plane.config.replica.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class PostgresqlStorageEngineProvisioner extends Ec2InstanceProvisioner {

    public PostgresqlStorageEngineProvisioner(Ec2Client ec2Client, ReplicaConfig postgresqlReplicaConfig) {
        super(ec2Client, postgresqlReplicaConfig);
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.storageEngineSecurityGroupId();
    }
}