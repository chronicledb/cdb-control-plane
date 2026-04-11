package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.replica.ReplicaConfig;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public abstract class Ec2InstanceProvisioner {
    protected final Ec2Client ec2Client;
    protected final ReplicaConfig replicaConfig;

    protected Ec2InstanceProvisioner(Ec2Client ec2Client, ReplicaConfig replicaConfig) {
        this.ec2Client = ec2Client;
        this.replicaConfig = replicaConfig;
    }

    protected abstract String getSecurityGroupId();

    protected String getUserData() {
        return "";
    }

    public String provision(String tagName) {
        final String encodedUserData = Base64.getEncoder()
                .encodeToString(getUserData().getBytes(StandardCharsets.UTF_8));

        return ec2Client.runInstances(RunInstancesRequest.builder()
                        .imageId(replicaConfig.amiId())
                        .instanceType(replicaConfig.instanceType())
                        .minCount(1)
                        .maxCount(1)
                        .userData(encodedUserData)
                        .networkInterfaces(InstanceNetworkInterfaceSpecification.builder()
                                .associatePublicIpAddress(true)
                                .subnetId(replicaConfig.subnetId())
                                .groups(getSecurityGroupId())
                                .deviceIndex(0)
                                .build())
                        .iamInstanceProfile(IamInstanceProfileSpecification.builder()
                                .name(replicaConfig.iamInstanceProfileName())
                                .build())
                        .tagSpecifications(TagSpecification.builder()
                                .resourceType(ResourceType.INSTANCE)
                                .tags(Tag.builder().key("Name").value(tagName).build())
                                .build())
                        .build())
                .instances().get(0).instanceId();
    }
}
