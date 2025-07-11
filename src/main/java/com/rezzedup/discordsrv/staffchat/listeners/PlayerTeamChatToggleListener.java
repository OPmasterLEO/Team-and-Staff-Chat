/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoTeamChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingTeamChatToggleEvent;
import community.leaf.eventful.bukkit.CancellationPolicy;
import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.CancelledEvents;
import community.leaf.eventful.bukkit.annotations.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.tlinkowski.annotation.basic.NullOr;

public class PlayerTeamChatToggleListener implements Listener {
	private final StaffChatPlugin plugin;
	
	public PlayerTeamChatToggleListener(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventListener(ListenerOrder.FIRST)
	public void onAutomaticChatFirst(AsyncPlayerChatEvent event) {
		if (plugin.data().isAutomaticTeamChatEnabled(event.getPlayer())) {
			event.setCancelled(true); // Cancel this message from getting sent to global chat.
			// Handle message in a later listener order, allowing other plugins to modify the message.
		}
	}
	
	@EventListener(ListenerOrder.MONITOR)
	public void onAutomaticChatMonitor(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (!plugin.data().isAutomaticTeamChatEnabled(player)) {
			return;
		}
		
		event.setCancelled(true); // Cancel this message from getting sent to global chat.
		// The event could've been uncancelled since cancelling it the first time.
		
		if (Permissions.TEAM_ACCESS.allows(player)) {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic team-chat enabled"
			);
			
			// Handle this on the main thread next tick.
			plugin.sync().run(() -> plugin.submitTeamMessageFromPlayer(event.getPlayer(), event.getMessage()));
		} else {
			plugin.debug(getClass()).log(event, () ->
				"Player " + player.getName() + " has automatic team-chat enabled " +
					"but they don't have permission to use the team chat"
			);
			
			// Remove this non-team profile (but in sync 'cus it calls an event).
			plugin.sync().run(() -> {
				plugin.data().updateProfile(player);
				player.chat(event.getMessage());
			});
		}
	}
	
	@EventListener(ListenerOrder.LAST)
	@CancelledEvents(CancellationPolicy.REJECT)
	public void onToggleAutoChat(AutoTeamChatToggleEvent event) {
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
		
		// Leaving is disabled, cancel the event.
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
