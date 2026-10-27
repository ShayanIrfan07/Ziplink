package com.ziplink.url.click;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
class ClickStoreTest {

	@Mock
	private ClickEventRepository clickEvents;
	@Mock
	private ShortUrlRepository shortUrls;

	@Test
	void storesTheEventIdAndDropsTheReferrerQuery() {
		UUID eventId = UUID.randomUUID();
		UUID linkId = UUID.randomUUID();
		ShortUrl url = ShortUrl.create(
				new Guest(UUID.randomUUID(), "b".repeat(64), Instant.parse("2026-10-26T10:00:00Z")),
				"https://example.com",
				"abc2345",
				null,
				null,
				Instant.parse("2026-10-26T10:00:00Z"));
		when(clickEvents.existsById(eventId)).thenReturn(false);
		when(shortUrls.findById(linkId)).thenReturn(Optional.of(url));
		ClickStore store = store();

		store.save(new StoredClick(
				eventId,
				linkId,
				Instant.parse("2026-10-26T10:15:00Z"),
				"https://News.Example/story?token=secret",
				ClientCategory.DESKTOP));

		ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
		verify(clickEvents).save(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(eventId);
		assertThat(captor.getValue().getReferrer()).isEqualTo("https://news.example/story");
	}

	@Test
	void skipsADuplicateDeliveryAndAMissingLink() {
		UUID eventId = UUID.randomUUID();
		UUID missingLink = UUID.randomUUID();
		when(clickEvents.existsById(eventId)).thenReturn(true);
		when(clickEvents.existsById(missingLink)).thenReturn(false);
		when(shortUrls.findById(missingLink)).thenReturn(Optional.empty());
		ClickStore store = store();

		store.save(sample(eventId, UUID.randomUUID()));
		store.save(sample(missingLink, missingLink));

		verify(clickEvents, never()).save(any());
		verify(shortUrls, never()).findById(eventId);
	}

	private ClickStore store() {
		return new ClickStore(clickEvents, shortUrls, new ClientClassifier());
	}

	private static StoredClick sample(UUID eventId, UUID linkId) {
		return new StoredClick(eventId, linkId, Instant.parse("2026-10-26T10:15:00Z"), null, ClientCategory.UNKNOWN);
	}
}
