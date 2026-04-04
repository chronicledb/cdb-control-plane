package io.github.grantchen2003.cdb.control.plane.associations;

public class ForbiddenAssociationException extends RuntimeException {
    public ForbiddenAssociationException() {
        super("You do not have permission to access this resource");
    }
}