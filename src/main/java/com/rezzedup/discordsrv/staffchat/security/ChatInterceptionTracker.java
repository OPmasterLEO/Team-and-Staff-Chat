/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.security;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInterceptionTracker {
	private static final long INTERCEPT_TTL_MS = 5_000L;
	private static final ConcurrentHashMap<UUID, Long> INTERCEPTED = new ConcurrentHashMap<>(64);

	private ChatInterceptionTracker() {
	}

	public static void markIntercepted(UUID playerId) {
		INTERCEPTED.put(playerId, System.currentTimeMillis());
	}

	public static boolean wasRecentlyIntercepted(UUID playerId) {
		Long markedAt = INTERCEPTED.get(playerId);
		if (markedAt == null) {
			return false;
		}
		long age = System.currentTimeMillis() - markedAt;
		if (age > INTERCEPT_TTL_MS) {
			INTERCEPTED.remove(playerId, markedAt);
			return false;
		}
		return true;
	}

	public static void clear(UUID playerId) {
		INTERCEPTED.remove(playerId);
	}
}
