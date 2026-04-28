package io.github.grantchen2003.cdb.control.plane.views;

public class ViewInUseException extends RuntimeException {
    public ViewInUseException(String message) {
        super(message);
    }
}
