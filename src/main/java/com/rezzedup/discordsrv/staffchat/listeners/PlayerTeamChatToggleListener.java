/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoTeamChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingTeamChatToggleEvent;
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

public class PlayerTeamChatToggleListener implements Listener {
	private final StaffChatPlugin plugin;

	public PlayerTeamChatToggleListener(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}

	@EventListener(ListenerOrder.FIRST)
	public void onAutomaticChatFirst(AsyncChatEvent event) {
		if (plugin.data().isAutomaticTeamChatEnabled(event.getPlayer())) {
			ChatInterceptionHelper.blockPublicChat(event, event.getPlayer());
		}
	}

	@EventListener(ListenerOrder.MONITOR)
	public void onAutomaticChatMonitor(AsyncChatEvent event) {
		Player player = event.getPlayer();
		if (!plugin.data().isAutomaticTeamChatEnabled(player)) {
			return;
		}

		ChatInterceptionHelper.blockPublicChat(event, player);
		String message = ChatText.plain(event.message());

		if (Permissions.TEAM_ACCESS.allows(player)) {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic team-chat enabled"
			);
			plugin.sync().run(() -> plugin.submitTeamMessageFromPlayer(player, message));
		} else {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic team-chat enabled " +
					"but they don't have permission to use the team chat"
			);
			plugin.sync().run(() -> {
				plugin.data().updateProfile(player);
				player.chat(message);
			});
		}
	}

	@EventListener(ListenerOrder.LAST)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onToggleAutoChat(AutoTeamChatToggleEvent event) {
		plugin.invalidatePlayerCache();

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			String enabled = (event.isEnablingAutomaticChat()) ? "Enabled" : "Disabled";
			return enabled + " automatic team-chat for player: " + name + " (" + event.getProfile().uuid() + ")";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		if (event.isEnablingAutomaticChat()) {
			plugin.messages().notifyAutoTeamChatEnabled(player);
		} else {
			plugin.messages().notifyAutoTeamChatDisabled(player);
		}
	}

	@EventListener(ListenerOrder.EARLY)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onLeavingTeamChatIsDisabled(ReceivingTeamChatToggleEvent event) {
		if (event.isJoiningTeamChat()) {
			return;
		}
		if (plugin.config().getOrDefault(StaffChatConfig.LEAVING_TEAMCHAT_ENABLED)) {
			return;
		}

		event.setCancelled(true);

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			return "Player: " + name + " (" + event.getProfile().uuid() + ") " +
				"tried to leave the team chat, but leaving is disabled in the config";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		plugin.messages().notifyLeavingTeamChatIsDisabled(player);
	}

	@EventListener(ListenerOrder.LAST)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onToggleReceivingMessages(ReceivingTeamChatToggleEvent event) {
		plugin.invalidatePlayerCache();

		@NullOr Player player = event.getProfile().toPlayer().orElse(null);

		plugin.debug(getClass()).log(event, () -> {
			String name = (player == null) ? "<Offline>" : player.getName();
			String left = (event.isLeavingTeamChat()) ? "left" : "joined";
			return "Player: " + name + " (" + event.getProfile().uuid() + ") " + left + " the team-chat";
		});

		if (player == null || event.isQuiet()) {
			return;
		}

		boolean broadcastToEveryone =
			event.getProfile().sinceLeftTeamChat().isPresent() != event.isLeavingTeamChat();

		if (event.isLeavingTeamChat()) {
			plugin.messages().notifyLeaveTeamChat(player, broadcastToEveryone);
		} else {
			plugin.messages().notifyJoinTeamChat(player, broadcastToEveryone);
		}
	}
}
