package com.ziplink.url;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

	static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	static final int LENGTH = 7;
	private final SecureRandom random = new SecureRandom();

	public String randomCode() {
		StringBuilder code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}
}
