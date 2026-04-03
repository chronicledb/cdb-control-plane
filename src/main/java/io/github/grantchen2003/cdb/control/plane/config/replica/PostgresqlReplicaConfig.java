package io.github.grantchen2003.cdb.control.plane.config.replica;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.replica.postgresql")
public record PostgresqlReplicaConfig(
        String amiId,
        String instanceType,
        String subnetId,
        String txManagerSecurityGroupId,
        String storageEngineSecurityGroupId,
        String applierSecurityGroupId,
        String iamInstanceProfileName
) implements ReplicaConfig {}