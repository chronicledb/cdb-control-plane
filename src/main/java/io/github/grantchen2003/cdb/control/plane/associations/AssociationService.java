package io.github.grantchen2003.cdb.control.plane.associations;

import org.springframework.stereotype.Service;

@Service
public class AssociationService {
    private final AssociationRepository associationRepository;

    public AssociationService(AssociationRepository associationRepository) {
        this.associationRepository = associationRepository;
    }

    public Association createAssociation(String replicaId, String viewId) {
        final Association association = new Association(replicaId, viewId);

        associationRepository.save(new Association(replicaId, viewId));

        return association;
    }
}
