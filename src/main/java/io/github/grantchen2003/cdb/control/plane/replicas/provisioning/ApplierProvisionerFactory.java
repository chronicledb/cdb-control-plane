package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql.PostgresqlApplierProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisApplierProvisioner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Component
public class ApplierProvisionerFactory {
    private final Ec2Client ec2Client;
    private final PostgresqlReplicaConfig postgresqlReplicaConfig;
    private final RedisReplicaConfig redisReplicaConfig;

    public ApplierProvisionerFactory(
            Ec2Client ec2Client,
            PostgresqlReplicaConfig postgresqlReplicaConfig,
            RedisReplicaConfig redisReplicaConfig
    ) {
        this.ec2Client = ec2Client;
        this.postgresqlReplicaConfig = postgresqlReplicaConfig;
        this.redisReplicaConfig = redisReplicaConfig;
    }

    public Ec2InstanceProvisioner forType(ReplicaType replicaType) {
        return switch (replicaType) {
            case POSTGRESQL -> new PostgresqlApplierProvisioner(ec2Client, postgresqlReplicaConfig);
            case REDIS -> new RedisApplierProvisioner(ec2Client, redisReplicaConfig);
        };
    }
}
