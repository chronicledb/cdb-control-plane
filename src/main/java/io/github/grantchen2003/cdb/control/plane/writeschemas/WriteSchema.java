package io.github.grantchen2003.cdb.control.plane.writeschemas;

import java.time.Instant;

public record WriteSchema(
        String id,
        String userId,
        String chronicleName,
        String writeSchemaJson,
        Instant createdAt
) {}