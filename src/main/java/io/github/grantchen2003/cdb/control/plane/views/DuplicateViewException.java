package io.github.grantchen2003.cdb.control.plane.views;

public class DuplicateViewException extends RuntimeException {
    public DuplicateViewException() {
        super("View already exists");
    }
}