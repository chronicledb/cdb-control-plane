package io.github.grantchen2003.cdb.control.plane.readschemas;

import java.time.Instant;

public record ReadSchema(
        String id,
        String userId,
        String chronicleName,
        String viewName,
        String readSchemaJson,
        Instant createdAt
) {}
