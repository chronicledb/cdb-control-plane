package io.github.grantchen2003.cdb.control.plane.chronicles;

import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchema;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ChronicleService {

    private static final Logger log = LoggerFactory.getLogger(ChronicleService.class);

    private final ChronicleRepository chronicleRepository;
    private final WriteSchemaService writeSchemaService;

    public ChronicleService(ChronicleRepository chronicleRepository, WriteSchemaService writeSchemaService) {
        this.chronicleRepository = chronicleRepository;
        this.writeSchemaService = writeSchemaService;
    }

    public Chronicle createChronicle(String userId, String name, String writeSchemaJson) {
        final WriteSchema writeSchema = writeSchemaService.createWriteSchema(userId, name, writeSchemaJson);

        final Chronicle chronicle = new Chronicle(userId, name, writeSchema.id(), Instant.now());

        try {
            chronicleRepository.save(chronicle);
        } catch (Exception e) {
            log.error("Chronicle save failed after schema save — orphaned schema for userId: {} chronicleName: {}", userId, name);
            throw e;
        }

        return chronicle;
    }

    public boolean existsByUserIdAndName(String userId, String name) {
        return chronicleRepository.existsByUserIdAndName(userId, name);
    }
}