package io.github.grantchen2003.cdb.control.plane.config.replica;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.replica.redis")
public record RedisReplicaConfig(
        String amiId,
        String instanceType,
        String subnetId,
        int txManagerPort,
        String txManagerSecurityGroupId,
        String storageEngineSecurityGroupId,
        int applierPort,
        String applierSecurityGroupId,
        String iamInstanceProfileName
) implements ReplicaConfig {}