package io.github.grantchen2003.cdb.control.plane.chronicles;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ChronicleService {

    private final ChronicleRepository chronicleRepository;

    public ChronicleService(ChronicleRepository chronicleRepository) {
        this.chronicleRepository = chronicleRepository;
    }

    public Chronicle createChronicle(String userId, String name) {
        final Chronicle chronicle = new Chronicle(
                UUID.randomUUID().toString(),
                userId,
                name,
                Instant.now()
        );

        chronicleRepository.save(chronicle);

        return chronicle;
    }
}