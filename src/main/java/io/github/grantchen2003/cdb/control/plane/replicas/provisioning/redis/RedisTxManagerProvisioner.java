package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisTxManagerProvisioner extends Ec2InstanceProvisioner {

    private final AwsConfig awsConfig;
    private final String chronicleId;
    private final String writeSchemaJson;
    private final String chronicleServiceIp;
    private final int chronicleServicePort;

    public RedisTxManagerProvisioner(
            AwsConfig awsConfig,
            Ec2Client ec2Client,
            RedisReplicaConfig redisReplicaConfig,
            String chronicleId,
            String writeSchemaJson,
            String chronicleServiceIp,
            int chronicleServicePort) {
        super(ec2Client, redisReplicaConfig);
        this.awsConfig = awsConfig;
        this.chronicleId = chronicleId;
        this.writeSchemaJson = writeSchemaJson;
        this.chronicleServiceIp = chronicleServiceIp;
        this.chronicleServicePort = chronicleServicePort;
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
                                " -e CHRONICLE_ID='%s'" +
                                " -e CHRONICLE_SERVICE_IP='%s'" +
                                " -e CHRONICLE_SERVICE_PORT='%d'" +
                                " -e TX_MANAGER_PORT='%d'" +
                                " -e WRITE_SCHEMA_JSON='%s'" +
                                " %s",
                        replicaConfig.txManagerPort(), replicaConfig.txManagerPort(),
                        chronicleId, chronicleServiceIp, chronicleServicePort, replicaConfig.txManagerPort(),
                        writeSchemaJson.replace("'", "'\\''"),  // escape single quotes in JSON
                        imageUri)
        );
    }
}
