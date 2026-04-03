package io.github.grantchen2003.cdb.control.plane.config.replica;

public interface ReplicaConfig {String amiId();
    String instanceType();
    String subnetId();
    String iamInstanceProfileName();
    String txManagerSecurityGroupId();
    String storageEngineSecurityGroupId();
    String applierSecurityGroupId();
}
