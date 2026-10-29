package com.ziplink.url;

import java.net.URI;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ziplink.common.exception.ApiException;
import com.ziplink.guest.Guest;
import com.ziplink.guest.GuestService;
import com.ziplink.identity.CurrentAccount;
import com.ziplink.identity.UserAccount;
import com.ziplink.url.dto.CreateUrlRequest;
import com.ziplink.url.dto.PageResponse;
import com.ziplink.url.dto.UpdateUrlRequest;
import com.ziplink.url.dto.UrlResponse;

@RestController
public class UrlController {

	private final UrlService urls;
	private final GuestService guests;
	private final CurrentAccount accounts;

	public UrlController(UrlService urls, GuestService guests, CurrentAccount accounts) {
		this.urls = urls;
		this.guests = guests;
		this.accounts = accounts;
	}

	@PostMapping("/api/v1/urls")
	public ResponseEntity<UrlResponse> create(
			@Valid @RequestBody CreateUrlRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		UrlResponse created = accounts.currentReference()
				.map(user -> urls.createForUser(user, request, clientKey(httpRequest)))
				.orElseGet(() -> urls.create(guests.resolveOrCreate(httpRequest, httpResponse), request, clientKey(httpRequest)));
		return ResponseEntity.created(URI.create("/api/v1/urls/" + created.id())).body(created);
	}

	@GetMapping("/api/v1/urls")
	public PageResponse<UrlResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			@RequestParam(required = false) String q,
			HttpServletRequest request) {
		return accounts.currentReference()
				.map(user -> urls.list(LinkScope.user(user.getId()), page, size, sort, q))
				.orElseGet(() -> urls.list(guests.find(request).orElse(null), page, size, sort, q));
	}

	@GetMapping("/api/v1/urls/{id}")
	public UrlResponse get(@PathVariable UUID id, HttpServletRequest request) {
		return urls.get(scope(request), id);
	}

	@PatchMapping("/api/v1/urls/{id}")
	public UrlResponse update(
			@PathVariable UUID id,
			@RequestBody UpdateUrlRequest requestBody,
			HttpServletRequest request) {
		return urls.update(scope(request), id, requestBody);
	}

	@DeleteMapping("/api/v1/urls/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
		urls.delete(scope(request), id);
		return ResponseEntity.noContent().build();
	}

	private LinkScope scope(HttpServletRequest request) {
		return accounts.currentReference()
				.map(UserAccount::getId)
				.map(LinkScope::user)
				.orElseGet(() -> guests.find(request)
						.map(Guest::getId)
						.map(LinkScope::guest)
						.orElseThrow(() -> ApiException.notFound("Link not found")));
	}

	private static String clientKey(HttpServletRequest request) {
		String address = request.getRemoteAddr();
		return address == null || address.isBlank() ? "unknown" : address;
	}

}
