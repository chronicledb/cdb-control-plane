package io.github.grantchen2003.cdb.control.plane.writeschemas;

import io.github.grantchen2003.cdb.control.plane.writeschemas.validators.WriteSchemaValidator;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WriteSchemaService {

    private final WriteSchemaRepository writeSchemaRepository;
    private final WriteSchemaValidator writeSchemaValidator;

    public WriteSchemaService(WriteSchemaRepository writeSchemaRepository, WriteSchemaValidator writeSchemaValidator) {
        this.writeSchemaRepository = writeSchemaRepository;
        this.writeSchemaValidator = writeSchemaValidator;
    }

    public WriteSchema createWriteSchema(String userId, String chronicleName, String writeSchemaJson) {
        writeSchemaValidator.validate(writeSchemaJson);

        final WriteSchema writeSchema = new WriteSchema(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                writeSchemaJson,
                Instant.now()
        );

        writeSchemaRepository.save(writeSchema);

        return writeSchema;
    }

    public Optional<WriteSchema> findByUserIdAndChronicleName(String userId, String chronicleName) {
        return writeSchemaRepository.findByUserIdAndChronicleName(userId, chronicleName);
    }
}