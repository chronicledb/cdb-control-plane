package io.github.grantchen2003.cdb.control.plane.writeschemas;

public class InvalidWriteSchemaException extends RuntimeException {
    public InvalidWriteSchemaException(String reason) {
        super("Invalid write schema: " + reason);
    }
}
