package io.github.grantchen2003.cdb.control.plane.replicas;

public class ReplicaInUseException extends RuntimeException {
    public ReplicaInUseException(String message) {
        super(message);
    }
}
