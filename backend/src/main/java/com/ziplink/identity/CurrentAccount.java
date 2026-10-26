package com.ziplink.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccount {

	private final UserAccountRepository users;

	public CurrentAccount(UserAccountRepository users) {
		this.users = users;
	}

	public Optional<UserAccount> currentReference() {
		return currentId().map(users::getReferenceById);
	}

	public Optional<UUID> currentId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
			return Optional.empty();
		}
		return Optional.of(user.id());
	}

	public UUID requireId() {
		return currentId().orElseThrow(() -> com.ziplink.common.exception.ApiException.unauthorized("Sign in to continue"));
	}
}
