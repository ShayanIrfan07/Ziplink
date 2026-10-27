package com.ziplink.url.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClickRecorderTest {

	@Mock
	private ClickDispatch dispatch;

	@Test
	void publishesABrowserClickWithoutTheReferrerQuery() {
		UUID id = UUID.randomUUID();
		ClickRecorder recorder = recorder();

		recorder.record(id, new ClientSignals(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
				"https://News.Example/story?token=secret#part",
				null));

		ArgumentCaptor<StoredClick> captor = ArgumentCaptor.forClass(StoredClick.class);
		verify(dispatch).dispatch(captor.capture());
		assertThat(captor.getValue().shortUrlId()).isEqualTo(id);
		assertThat(captor.getValue().clickedAt()).isEqualTo(Instant.parse("2026-10-26T10:15:00Z"));
		assertThat(captor.getValue().referrer()).isEqualTo("https://news.example/story");
		assertThat(captor.getValue().category()).isEqualTo(ClientCategory.DESKTOP);
	}

	@Test
	void ignoresBotsAndPrefetch() {
		ClickRecorder recorder = recorder();
		UUID id = UUID.randomUUID();

		recorder.record(id, new ClientSignals("curl/8.4.0", null, null));
		recorder.record(id, new ClientSignals("Mozilla/5.0", null, "prefetch"));

		verify(dispatch, never()).dispatch(org.mockito.ArgumentMatchers.any());
	}

	private ClickRecorder recorder() {
		return new ClickRecorder(
				dispatch,
				new ClientClassifier(),
				Clock.fixed(Instant.parse("2026-10-26T10:15:00Z"), ZoneOffset.UTC));
	}
}
