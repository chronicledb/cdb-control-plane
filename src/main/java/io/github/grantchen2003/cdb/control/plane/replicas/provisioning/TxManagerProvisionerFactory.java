package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql.PostgresqlTxManagerProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisTxManagerProvisioner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Component
public class TxManagerProvisionerFactory {
    @Value("${cdb.chronicle.service.ip}")
    private String chronicleServiceIp;

    @Value("${cdb.chronicle.service.port}")
    private int chronicleServicePort;

    private final AwsConfig awsConfig;
    private final Ec2Client ec2Client;
    private final PostgresqlReplicaConfig postgresqlReplicaConfig;
    private final RedisReplicaConfig redisReplicaConfig;

    public TxManagerProvisionerFactory(
            AwsConfig awsConfig,
            Ec2Client ec2Client,
            PostgresqlReplicaConfig postgresqlReplicaConfig,
            RedisReplicaConfig redisReplicaConfig
    ) {
        this.awsConfig = awsConfig;
        this.ec2Client = ec2Client;
        this.postgresqlReplicaConfig = postgresqlReplicaConfig;
        this.redisReplicaConfig = redisReplicaConfig;
    }

    public Ec2InstanceProvisioner forType(ReplicaType replicaType, String chronicleId, String writeSchemaJson, String storageEngineHost) {
        return switch (replicaType) {
            case POSTGRESQL -> new PostgresqlTxManagerProvisioner(ec2Client, postgresqlReplicaConfig);
            case REDIS -> new RedisTxManagerProvisioner(awsConfig, ec2Client, redisReplicaConfig, chronicleId,
                    writeSchemaJson, chronicleServiceIp, chronicleServicePort, storageEngineHost);
        };
    }
}