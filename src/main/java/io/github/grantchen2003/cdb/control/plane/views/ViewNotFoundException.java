package io.github.grantchen2003.cdb.control.plane.views;

public class ViewNotFoundException extends RuntimeException {
    public ViewNotFoundException() {
        super("View not found");
    }
}