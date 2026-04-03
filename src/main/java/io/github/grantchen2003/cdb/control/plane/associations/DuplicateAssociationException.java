package io.github.grantchen2003.cdb.control.plane.associations;

public class DuplicateAssociationException extends RuntimeException {
  public DuplicateAssociationException(String replicaId, String viewId) {
    super("Association between replica " + replicaId + " and view " + viewId + " already exists");
  }
}
