package io.github.grantchen2003.cdb.control.plane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.replica")
public record ReplicaConfig(
        String amiId,
        String instanceType,
        String subnetId,
        String securityGroupId
) {}