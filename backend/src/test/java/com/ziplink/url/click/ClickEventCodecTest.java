package com.ziplink.url.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClickEventCodecTest {

	@Test
	void roundTripsAReferrerThatNeedsEscaping() {
		StoredClick click = new StoredClick(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				Instant.parse("2026-10-26T10:15:00Z"),
				"https://news.example/a\"b\\c",
				ClientCategory.MOBILE);

		StoredClick decoded = ClickEventCodec.decode(ClickEventCodec.encode(click));

		assertThat(decoded).isEqualTo(click);
	}

	@Test
	void roundTripsAMissingReferrer() {
		StoredClick click = new StoredClick(
				UUID.randomUUID(),
				UUID.randomUUID(),
				Instant.parse("2026-10-27T00:00:00Z"),
				null,
				ClientCategory.UNKNOWN);

		assertThat(ClickEventCodec.decode(ClickEventCodec.encode(click))).isEqualTo(click);
	}

	@Test
	void rejectsABotAndABrokenPayload() {
		String bot = ClickEventCodec.encode(new StoredClick(
				UUID.randomUUID(),
				UUID.randomUUID(),
				Instant.parse("2026-10-27T00:00:00Z"),
				null,
				ClientCategory.DESKTOP)).replace("DESKTOP", "BOT");

		assertThatThrownBy(() -> ClickEventCodec.decode(bot))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ClickEventCodec.decode("not-json"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ClickEventCodec.decode(null))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
