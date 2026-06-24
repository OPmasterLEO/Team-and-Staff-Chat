/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.security;

import org.bukkit.entity.Player;

import io.papermc.paper.event.player.AsyncChatEvent;

public final class ChatInterceptionHelper {
	private ChatInterceptionHelper() {
	}

	public static void blockPublicChat(AsyncChatEvent event, Player player) {
		ChatInterceptionTracker.markIntercepted(player.getUniqueId());
		event.setCancelled(true);
		event.viewers().clear();
	}
}
