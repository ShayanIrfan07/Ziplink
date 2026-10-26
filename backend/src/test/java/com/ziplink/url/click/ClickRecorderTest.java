package com.ziplink.url.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ziplink.guest.Guest;
import com.ziplink.url.ShortUrl;
import com.ziplink.url.ShortUrlRepository;

@ExtendWith(MockitoExtension.class)
class ClickRecorderTest {

	@Mock
	private ClickEventRepository clickEvents;
	@Mock
	private ShortUrlRepository shortUrls;

	@Test
	void storesABrowserClickWithoutTheReferrerQuery() {
		UUID id = UUID.randomUUID();
		ShortUrl url = ShortUrl.create(
				new Guest(UUID.randomUUID(), "b".repeat(64), Instant.parse("2026-10-26T10:00:00Z")),
				"https://example.com",
				"abc2345",
				null,
				null,
				Instant.parse("2026-10-26T10:00:00Z"));
		when(shortUrls.findById(id)).thenReturn(Optional.of(url));
		ClickRecorder recorder = recorder();

		recorder.record(id, new ClientSignals(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
				"https://News.Example/story?token=secret#part",
				null));

		ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
		verify(clickEvents).save(captor.capture());
		assertThat(captor.getValue().getReferrer()).isEqualTo("https://news.example/story");
		assertThat(captor.getValue().getClientCategory()).isEqualTo(ClientCategory.DESKTOP);
	}

	@Test
	void ignoresBotsAndPrefetch() {
		ClickRecorder recorder = recorder();
		UUID id = UUID.randomUUID();

		recorder.record(id, new ClientSignals("curl/8.4.0", null, null));
		recorder.record(id, new ClientSignals("Mozilla/5.0", null, "prefetch"));

		verify(clickEvents, never()).save(any());
		verify(shortUrls, never()).findById(any());
	}

	private ClickRecorder recorder() {
		return new ClickRecorder(
				clickEvents,
				shortUrls,
				new ClientClassifier(),
				Clock.fixed(Instant.parse("2026-10-26T10:15:00Z"), ZoneOffset.UTC));
	}
}
