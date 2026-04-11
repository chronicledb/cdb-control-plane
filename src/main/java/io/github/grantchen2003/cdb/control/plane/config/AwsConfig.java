package io.github.grantchen2003.cdb.control.plane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsConfig(String accountId, String region) {}
