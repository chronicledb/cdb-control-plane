package io.github.grantchen2003.cdb.control.plane.associations;

public class AssociationChroniclesMismatchException extends RuntimeException {
    public AssociationChroniclesMismatchException() {
        super("Replica and view must belong to the same chronicle");
    }
}