package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisApplierProvisioner extends Ec2InstanceProvisioner {

    private final AwsConfig awsConfig;
    private final RedisReplicaConfig redisReplicaConfig;
    private final String chronicleLogKafkaBootstrapServers;
    private final String chronicleId;
    private final String writeSchemaJson;
    private final String storageEngineHost;

    public RedisApplierProvisioner(
            AwsConfig awsConfig,
            Ec2Client ec2Client,
            RedisReplicaConfig redisReplicaConfig,
            String chronicleLogKafkaBootstrapServers,
            String chronicleId,
            String writeSchemaJson,
            String storageEngineHost) {
        super(ec2Client, redisReplicaConfig);
        this.awsConfig = awsConfig;
        this.redisReplicaConfig = redisReplicaConfig;
        this.chronicleLogKafkaBootstrapServers = chronicleLogKafkaBootstrapServers;
        this.chronicleId = chronicleId;
        this.writeSchemaJson = writeSchemaJson;
        this.storageEngineHost = storageEngineHost;
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.applierSecurityGroupId();
    }

    @Override
    protected String getUserData() {
        final String imageUri = String.format("%s.dkr.ecr.%s.amazonaws.com/cdb-appliers:redis",
                awsConfig.accountId(), awsConfig.region());

        return String.join("\n",
                "#!/bin/bash",
                "apt-get update -y",
                "apt-get install -y ca-certificates curl unzip",

                // Docker
                "install -m 0755 -d /etc/apt/keyrings",
                "curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc",
                "chmod a+r /etc/apt/keyrings/docker.asc",
                "echo \"deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable\" | tee /etc/apt/sources.list.d/docker.list > /dev/null",
                "apt-get update -y",
                "apt-get install -y docker-ce docker-ce-cli containerd.io",
                "systemctl start docker",
                "systemctl enable docker",

                // AWS CLI v2
                "curl -fsSL https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o /tmp/awscliv2.zip",
                "unzip /tmp/awscliv2.zip -d /tmp",
                "/tmp/aws/install",

                // Wait for IAM instance profile credentials to be available
                "until /usr/local/bin/aws sts get-caller-identity --region " + awsConfig.region() + " > /dev/null 2>&1; do",
                "  echo 'Waiting for IAM credentials...'",
                "  sleep 2",
                "done",

                // ECR login and pull
                String.format("aws ecr get-login-password --region %s | docker login --username AWS --password-stdin %s.dkr.ecr.%s.amazonaws.com",
                        awsConfig.region(), awsConfig.accountId(), awsConfig.region()),
                String.format("docker pull %s", imageUri),

                // Run — note quotes around values that may contain spaces/special chars
                String.format("docker run -d" +
                                " -p %d:%d" +
                                " -e CHRONICLE_LOG_KAFKA_BOOTSTRAP_SERVERS='%s'" +
                                " -e CHRONICLE_ID='%s'" +
                                " -e WRITE_SCHEMA_JSON='%s'" +
                                " -e APPLIER_PORT='%d'" +
                                " -e REDIS_HOST='%s'" +
                                " -e REDIS_PORT='%d'" +
                                " %s",
                        replicaConfig.applierPort(), replicaConfig.applierPort(),
                        chronicleLogKafkaBootstrapServers,
                        chronicleId,
                        writeSchemaJson,
                        replicaConfig.applierPort(),
                        storageEngineHost,
                        redisReplicaConfig.storageEnginePort(),
                        imageUri)
        );
    }
}
