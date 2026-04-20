package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.PostgresqlReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.postgresql.PostgresqlApplierProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisApplierProvisioner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Component
public class ApplierProvisionerFactory {

    @Value("${cdb.chronicle.log.kafka-bootstrap-servers}")
    private String chronicleLogKafkaBootstrapServers;

    private final AwsConfig awsConfig;
    private final Ec2Client ec2Client;
    private final PostgresqlReplicaConfig postgresqlReplicaConfig;
    private final RedisReplicaConfig redisReplicaConfig;

    public ApplierProvisionerFactory(
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

    public Ec2InstanceProvisioner forType(
            ReplicaType replicaType,
            String chronicleId,
            String writeSchemaJson,
            String storageEngineHost) {
        return switch (replicaType) {
            case POSTGRESQL -> new PostgresqlApplierProvisioner(ec2Client, postgresqlReplicaConfig);
            case REDIS -> new RedisApplierProvisioner(awsConfig, ec2Client, redisReplicaConfig,
                    chronicleLogKafkaBootstrapServers, chronicleId, writeSchemaJson, storageEngineHost);
        };
    }
}
