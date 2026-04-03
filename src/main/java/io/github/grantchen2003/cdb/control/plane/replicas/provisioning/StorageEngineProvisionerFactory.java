package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisStorageEngineProvisioner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Component
public class StorageEngineProvisionerFactory {
    private final Ec2Client ec2Client;
    private final ReplicaConfig replicaConfig;

    public StorageEngineProvisionerFactory(Ec2Client ec2Client, ReplicaConfig replicaConfig) {
        this.ec2Client = ec2Client;
        this.replicaConfig = replicaConfig;
    }

    public Ec2InstanceProvisioner forType(ReplicaType replicaType) {
        return switch (replicaType) {
            case REDIS -> new RedisStorageEngineProvisioner(ec2Client, replicaConfig);
        };
    }
}