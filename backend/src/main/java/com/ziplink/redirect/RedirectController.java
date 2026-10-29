package com.ziplink.redirect;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.ziplink.url.RedirectResolver;
import com.ziplink.url.RedirectTarget;
import com.ziplink.url.click.ClickRecorder;
import com.ziplink.url.click.ClientSignals;

@RestController
public class RedirectController {

	private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

	private final RedirectResolver redirects;
	private final ClickRecorder clicks;

	public RedirectController(RedirectResolver redirects, ClickRecorder clicks) {
		this.redirects = redirects;
		this.clicks = clicks;
	}

	/**
	 * Destinations can change, so browsers receive a temporary redirect and are
	 * told not to cache it.
	 */
	@GetMapping("/{shortCode:[A-Za-z0-9_-]{3,32}}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
		RedirectTarget target = redirects.resolve(shortCode);
		try {
			clicks.record(target.id(), signals(request));
		} catch (RuntimeException exception) {
			log.warn("Click recording was not scheduled for a successful redirect");
		}
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(target.destination()))
				.cacheControl(CacheControl.noStore())
				.build();
	}

	private static ClientSignals signals(HttpServletRequest request) {
		String purpose = request.getHeader("Purpose");
		if (purpose == null) {
			purpose = request.getHeader("Sec-Purpose");
		}
		return new ClientSignals(request.getHeader("User-Agent"), request.getHeader("Referer"), purpose);
	}
}
