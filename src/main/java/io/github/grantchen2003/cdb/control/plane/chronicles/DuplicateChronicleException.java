package io.github.grantchen2003.cdb.control.plane.chronicles;

public class DuplicateChronicleException extends RuntimeException {
    public DuplicateChronicleException(String userId, String name) {
        super("Chronicle '" + name + "' already exists for user " + userId);
    }
}
