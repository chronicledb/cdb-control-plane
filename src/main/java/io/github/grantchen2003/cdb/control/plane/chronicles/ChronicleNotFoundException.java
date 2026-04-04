package io.github.grantchen2003.cdb.control.plane.chronicles;

public class ChronicleNotFoundException extends RuntimeException {
    public ChronicleNotFoundException() {
        super("Chronicle not found");
    }
}