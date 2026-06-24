/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.rezzedup.discordsrv.staffchat.security.ChatInterceptionHelper;
import com.rezzedup.discordsrv.staffchat.security.ChatInterceptionTracker;

import io.papermc.paper.event.player.AsyncChatEvent;

public final class ChatLeakGuardListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onAsyncChat(AsyncChatEvent event) {
		if (ChatInterceptionTracker.wasRecentlyIntercepted(event.getPlayer().getUniqueId())) {
			ChatInterceptionHelper.blockPublicChat(event, event.getPlayer());
		}
	}
}
