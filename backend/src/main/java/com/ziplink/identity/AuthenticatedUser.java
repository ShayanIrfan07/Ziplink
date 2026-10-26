package com.ziplink.identity;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email) {
}
