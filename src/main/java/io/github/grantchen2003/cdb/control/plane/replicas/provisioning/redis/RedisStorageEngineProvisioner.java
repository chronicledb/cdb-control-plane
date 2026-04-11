package io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis;

import io.github.grantchen2003.cdb.control.plane.config.AwsConfig;
import io.github.grantchen2003.cdb.control.plane.config.replica.RedisReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class RedisStorageEngineProvisioner extends Ec2InstanceProvisioner {

    private final AwsConfig awsConfig;

    public RedisStorageEngineProvisioner(AwsConfig awsConfig, Ec2Client ec2Client, RedisReplicaConfig redisReplicaConfig) {
        super(ec2Client, redisReplicaConfig);
        this.awsConfig = awsConfig;
    }

    @Override
    protected String getSecurityGroupId() {
        return replicaConfig.storageEngineSecurityGroupId();
    }

    @Override
    protected String getUserData() {
        String imageUri = String.format("%s.dkr.ecr.%s.amazonaws.com/cdb-storage-engines:redis",
                awsConfig.accountId(), awsConfig.region());

        return String.join("\n",
                "#!/bin/bash",
                "set -e",
                "apt-get update -y",
                "apt-get install -y docker.io unzip",
                "curl https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip -o /tmp/awscliv2.zip",
                "unzip /tmp/awscliv2.zip -d /tmp && /tmp/aws/install",
                "systemctl enable docker && systemctl start docker",
                "aws ecr get-login-password --region " + awsConfig.region() + " | \\",
                "  docker login --username AWS --password-stdin " + awsConfig.accountId() + ".dkr.ecr." + awsConfig.region() + ".amazonaws.com",
                "docker pull " + imageUri,
                "docker run -d --name redis -p 6379:6379 --restart unless-stopped " + imageUri
        );
    }
}
