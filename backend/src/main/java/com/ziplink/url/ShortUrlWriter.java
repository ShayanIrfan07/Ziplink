package com.ziplink.url;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlWriter {

	private final ShortUrlRepository repository;

	public ShortUrlWriter(ShortUrlRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ShortUrl insert(ShortUrl url) {
		return repository.saveAndFlush(url);
	}
}
