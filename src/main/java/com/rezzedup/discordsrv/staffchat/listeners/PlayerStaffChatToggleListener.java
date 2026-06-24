/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.security.ChatInterceptionHelper;
import com.rezzedup.discordsrv.staffchat.util.ChatText;
import community.leaf.eventful.bukkit.CancellationPolicy;
import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.CancelledEvents;
import community.leaf.eventful.bukkit.annotations.EventListener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import pl.tlinkowski.annotation.basic.NullOr;

public class PlayerStaffChatToggleListener implements Listener {
	private final StaffChatPlugin plugin;

	public PlayerStaffChatToggleListener(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}

	@EventListener(ListenerOrder.FIRST)
	public void onAutomaticChatFirst(AsyncChatEvent event) {
		if (plugin.data().isAutomaticStaffChatEnabled(event.getPlayer())) {
			ChatInterceptionHelper.blockPublicChat(event, event.getPlayer());
		}
	}

	@EventListener(ListenerOrder.MONITOR)
	public void onAutomaticChatMonitor(AsyncChatEvent event) {
		Player player = event.getPlayer();
		if (!plugin.data().isAutomaticStaffChatEnabled(player)) {
			return;
		}

		ChatInterceptionHelper.blockPublicChat(event, player);
		String message = ChatText.plain(event.message());

		if (Permissions.ACCESS.allows(player)) {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic staff-chat enabled"
			);
			plugin.sync().run(() -> plugin.submitMessageFromPlayer(player, message));
		} else {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic staff-chat enabled " +
					"but they don't have permission to use the staff chat"
			);
			plugin.sync().run(() -> {
				plugin.data().updateProfile(player);
				player.chat(message);
			});
		}
	}

	@EventListener(ListenerOrder.LAST)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onToggleAutoChat(AutoStaffChatToggleEvent event) {
		plugin.invalidatePlayerCache();

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			String enabled = (event.isEnablingAutomaticChat()) ? "Enabled" : "Disabled";
			return enabled + " automatic staff-chat for player: " + name + " (" + event.getProfile().uuid() + ")";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		if (event.isEnablingAutomaticChat()) {
			plugin.messages().notifyAutoChatEnabled(player);
		} else {
			plugin.messages().notifyAutoChatDisabled(player);
		}
	}

	@EventListener(ListenerOrder.EARLY)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onLeavingStaffChatIsDisabled(ReceivingStaffChatToggleEvent event) {
		if (event.isJoiningStaffChat()) {
			return;
		}
		if (plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED)) {
			return;
		}

		event.setCancelled(true);

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			return "Player: " + name + " (" + event.getProfile().uuid() + ") " +
				"tried to leave the staff chat, but leaving is disabled in the config";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		plugin.messages().notifyLeavingChatIsDisabled(player);
	}

	@EventListener(ListenerOrder.LAST)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onToggleReceivingMessages(ReceivingStaffChatToggleEvent event) {
		plugin.invalidatePlayerCache();

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			String left = (event.isLeavingStaffChat()) ? "left" : "joined";
			return "Player: " + name + " (" + event.getProfile().uuid() + ") " + left + " the staff-chat";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		boolean broadcastToEveryone =
			event.getProfile().sinceLeftStaffChat().isPresent() != event.isLeavingStaffChat();

		if (event.isLeavingStaffChat()) {
			plugin.messages().notifyLeaveChat(player, broadcastToEveryone);
		} else {
			plugin.messages().notifyJoinChat(player, broadcastToEveryone);
		}
	}
}
