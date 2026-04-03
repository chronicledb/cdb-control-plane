package io.github.grantchen2003.cdb.control.plane.views;

import java.time.Instant;

public record View(
        String viewId,
        String userId,
        String chronicleName,
        String viewName,
        Instant createdAt
) {}
