package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisTxManagerProvisioner extends Ec2InstanceProvisioner {

    public RedisTxManagerProvisioner(Ec2Client ec2Client, RedisReplicaConfig redisReplicaConfig) {
        super(ec2Client, redisReplicaConfig);
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.txManagerSecurityGroupId();
    }
}
