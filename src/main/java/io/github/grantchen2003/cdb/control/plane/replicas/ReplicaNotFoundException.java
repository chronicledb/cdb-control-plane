package io.github.grantchen2003.cdb.control.plane.replicas;

public class ReplicaNotFoundException extends RuntimeException {
    public ReplicaNotFoundException() {
        super("Replica not found");
    }
}