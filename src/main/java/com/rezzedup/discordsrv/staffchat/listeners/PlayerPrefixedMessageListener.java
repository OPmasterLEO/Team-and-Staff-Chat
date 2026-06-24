/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.security.ChatInterceptionHelper;
import com.rezzedup.discordsrv.staffchat.util.ChatText;

import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.EventListener;
import io.papermc.paper.event.player.AsyncChatEvent;

public class PlayerPrefixedMessageListener implements Listener {
	private final StaffChatPlugin plugin;

	private volatile boolean staffChatEnabled;
	private volatile String staffChatPrefix;
	private volatile int staffPrefixLength;
	private volatile boolean teamChatEnabled;
	private volatile String teamChatPrefix;
	private volatile int teamPrefixLength;

	public PlayerPrefixedMessageListener(StaffChatPlugin plugin) {
		this.plugin = plugin;
		refreshCache();
	}

	public void refreshCache() {
		staffChatEnabled = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_ENABLED);
		staffChatPrefix = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_IDENTIFIER);
		staffPrefixLength = staffChatPrefix.length();
		teamChatEnabled = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_TEAM_CHAT_ENABLED);
		teamChatPrefix = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_TEAM_CHAT_IDENTIFIER);
		teamPrefixLength = teamChatPrefix.length();
	}

	@EventListener(ListenerOrder.EARLY)
	public void onChatMessage(AsyncChatEvent event) {
		Player sender = event.getPlayer();
		String message = ChatText.plain(event.message());

		if (staffChatEnabled && message.startsWith(staffChatPrefix) && Permissions.ACCESS.allows(sender)) {
			String content = message.substring(staffPrefixLength).trim();

			plugin.debug(getClass()).log(event, () ->
				"Prefixed staff-chat message from " + sender.getName() + ": " + content
			);

			ChatInterceptionHelper.blockPublicChat(event, sender);
			plugin.sync().run(() -> plugin.submitMessageFromPlayer(sender, content));
			return;
		}

		if (teamChatEnabled && message.startsWith(teamChatPrefix) && Permissions.TEAM_ACCESS.allows(sender)) {
			String content = message.substring(teamPrefixLength).trim();

			plugin.debug(getClass()).log(event, () ->
				"Prefixed team-chat message from " + sender.getName() + ": " + content
			);

			ChatInterceptionHelper.blockPublicChat(event, sender);
			plugin.sync().run(() -> plugin.submitTeamMessageFromPlayer(sender, content));
		}
	}
}
