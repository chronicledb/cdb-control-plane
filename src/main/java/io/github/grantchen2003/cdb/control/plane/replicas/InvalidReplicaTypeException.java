package io.github.grantchen2003.cdb.control.plane.replicas;

public class InvalidReplicaTypeException extends RuntimeException {
    public InvalidReplicaTypeException(String replicaType) {
        super("Invalid replica type: " + replicaType);
    }
}