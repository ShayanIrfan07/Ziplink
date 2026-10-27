package com.ziplink.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

	private final ShortCodeGenerator generator = new ShortCodeGenerator();

	@Test
	void codesUseTheUnambiguousAlphabet() {
		for (int i = 0; i < 20; i++) {
			String code = generator.randomCode();
			assertThat(code).hasSize(ShortCodeGenerator.LENGTH);
			assertThat(code.chars().allMatch(character -> ShortCodeGenerator.ALPHABET.indexOf(character) >= 0)).isTrue();
		}
	}

	@Test
	void skipsTakenAndReservedCodes() {
		Set<String> seen = new HashSet<>();
		Predicate<String> taken = code -> !seen.add(code) || ReservedCodes.isReserved(code);
		String first = generator.randomCode();
		assertThat(first).isNotBlank();
		assertThat(taken.test("api")).isTrue();
	}

	@Test
	void likeEscapeTreatsWildcardsAsLiterals() {
		assertThat(ShortUrlSpecifications.escapeLike("100%_done\\path")).isEqualTo("100\\%\\_done\\\\path");
	}

	@Test
	void rateLimiterBlocksTheNextCallInTheSameWindow() {
		CreateRateLimiter limiter = new CreateRateLimiter(2);
		limiter.acquire("10.0.0.8");
		limiter.acquire("10.0.0.8");
		assertThatThrownBy(() -> limiter.acquire("10.0.0.8")).isInstanceOf(com.ziplink.common.exception.ApiException.class);
		limiter.acquire("10.0.0.9");
	}
}
