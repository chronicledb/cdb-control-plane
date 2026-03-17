package io.github.grantchen2003.cdb.control.plane.users;

import java.time.Instant;

public record User(String id, String email, Instant createdAt) {}