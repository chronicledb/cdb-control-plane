package io.github.grantchen2003.cdb.control.plane.views;

import java.time.Instant;

public record View(
        String id,
        String userId,
        String chronicleName,
        String viewName,
        String readSchemaId,
        Instant createdAt
) {}
