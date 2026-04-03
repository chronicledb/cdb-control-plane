package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisTxManagerProvisioner extends Ec2InstanceProvisioner {

    public RedisTxManagerProvisioner(Ec2Client ec2Client, ReplicaConfig replicaConfig) {
        super(ec2Client, replicaConfig);
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.redisTxManagerSecurityGroupId();
    }
}
