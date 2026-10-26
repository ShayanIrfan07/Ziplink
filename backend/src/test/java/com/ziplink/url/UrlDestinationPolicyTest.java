package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import com.ziplink.common.exception.ApiException;
import com.ziplink.common.exception.ErrorCode;

class UrlDestinationPolicyTest {

	private final UrlDestinationPolicy policy = new UrlDestinationPolicy();

	@Test
	void acceptsHttpAndNormalizesHostAndScheme() {
		assertThat(policy.validate(" HTTPS://Example.COM/Docs/Page?q=1#install "))
				.isEqualTo("https://example.com/Docs/Page?q=1#install");
	}

	@Test
	void preservesEncodedCharacters() {
		assertThat(policy.validate("https://example.com/a%20b?x=%2F"))
				.isEqualTo("https://example.com/a%20b?x=%2F");
	}

	@Test
	void rejectsUnsupportedSchemesAndCredentials() {
		assertRejected("javascript:alert(1)");
		assertRejected("data:text/html,hi");
		assertRejected("file:///etc/passwd");
		assertRejected("ftp://example.com/file");
		assertRejected("https://user:secret@example.com");
		assertRejected("https:///missing-host");
		assertRejected("   ");
		assertRejected("https://example.com/\r\nLocation:https://evil.example");
	}

	@Test
	void rejectsOverlongUrls() {
		assertThatThrownBy(() -> policy.validate("https://example.com/" + "a".repeat(2030)))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	private void assertRejected(String value) {
		assertThatThrownBy(() -> policy.validate(value))
				.isInstanceOfSatisfying(ApiException.class, error ->
						assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}
}
