package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;

class AliasPolicyTest {

	private final AliasPolicy policy = new AliasPolicy();

	@Test
	void acceptsANormalAlias() {
		assertThat(policy.normalize("My-Link_1")).isEqualTo("My-Link_1");
	}

	@Test
	void rejectsReservedAndMalformedAliases() {
		assertThatThrownBy(() -> policy.normalize("api"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.ALIAS_RESERVED));
		assertThatThrownBy(() -> policy.normalize("Swagger-UI"))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.ALIAS_RESERVED));
		assertThatThrownBy(() -> policy.normalize("no"))
				.isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> policy.normalize("has space"))
				.isInstanceOf(ApiException.class);
	}
}
