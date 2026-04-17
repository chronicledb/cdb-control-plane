package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisTxManagerProvisioner extends Ec2InstanceProvisioner {

    private final AwsConfig awsConfig;
    private final String chronicleId;
    private final String writeSchemaJson;

    @Value("${cdb.chronicle.service.ip}")
    private String chronicleServiceIp;

    @Value("${cdb.chronicle.service.port}")
    private int chronicleServicePort;

    public RedisTxManagerProvisioner(
            AwsConfig awsConfig,
            Ec2Client ec2Client,
            RedisReplicaConfig redisReplicaConfig,
            String chronicleId,
            String writeSchemaJson) {
        super(ec2Client, redisReplicaConfig);
        this.awsConfig = awsConfig;
        this.chronicleId = chronicleId;
        this.writeSchemaJson = writeSchemaJson;
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.txManagerSecurityGroupId();
    }

    @Override
    protected String getUserData() {
        final String imageUri = String.format("%s.dkr.ecr.%s.amazonaws.com/cdb-tx-managers:redis",
                awsConfig.accountId(), awsConfig.region());

        return String.join("\n",
                "#!/bin/bash",
                "apt-get update -y",
                "apt-get install -y docker.io awscli",
                "systemctl start docker",
                "systemctl enable docker",
                String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                        awsConfig.region(), awsConfig.accountId(), awsConfig.region()),
                String.format("docker pull %s", imageUri),
                "docker run -d \\",
                String.format("  -e CHRONICLE_ID=%s \\", chronicleId),
                String.format("  -e CHRONICLE_SERVICE_IP=%s \\", chronicleServiceIp),
                String.format("  -e CHRONICLE_SERVICE_PORT=%d \\", chronicleServicePort),
                String.format("  -e TX_MANAGER_PORT=%d \\", 8080),
                String.format("  -e WRITE_SCHEMA_JSON=%s \\", writeSchemaJson),
                String.format("  %s", imageUri)
        );
    }
}
