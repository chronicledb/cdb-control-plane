package io.github.grantchen2003.cdb.control.plane.readschemas;

public class InvalidReadSchemaException extends RuntimeException {
    public InvalidReadSchemaException(String reason) {
        super("Invalid read schema: " + reason);
    }
}
