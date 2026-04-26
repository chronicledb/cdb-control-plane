package io.github.grantchen2003.cdb.control.plane.readschemas;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReadSchemaService {

    private final ReadSchemaRepository readSchemaRepository;

    public ReadSchemaService(ReadSchemaRepository readSchemaRepository) {
        this.readSchemaRepository = readSchemaRepository;
    }

    public ReadSchema createReadSchema(String userId, String chronicleName, String viewName, String readSchemaJson) {
        // TODO: validate readSchemaJson

        final ReadSchema readSchema = new ReadSchema(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                viewName,
                readSchemaJson,
                Instant.now()
        );

        readSchemaRepository.save(readSchema);

        return readSchema;
    }
}
