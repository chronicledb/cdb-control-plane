package io.github.grantchen2003.cdb.control.plane.chronicles;

import java.time.Instant;

public record Chronicle(String id, String userId, String name, Instant createdAt) {}
