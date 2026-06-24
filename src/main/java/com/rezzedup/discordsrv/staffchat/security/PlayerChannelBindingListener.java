/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.security;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;

import io.netty.channel.Channel;
import pl.tlinkowski.annotation.basic.NullOr;

final class PlayerChannelBindingListener implements Listener {
	private final StaffChatPlugin plugin;

	PlayerChannelBindingListener(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		bind(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		ChatInterceptionTracker.clear(event.getPlayer().getUniqueId());
	}

	private void bind(Player player) {
		@NullOr Channel channel = PlayerChannels.resolve(player);
		if (channel != null) {
			OutgoingChatPacketGuard.bindPlayer(channel, player.getUniqueId());
			return;
		}
		plugin.sync().delay(1).ticks().run(() -> {
			@NullOr Channel delayed = PlayerChannels.resolve(player);
			if (delayed != null) {
				OutgoingChatPacketGuard.bindPlayer(delayed, player.getUniqueId());
			}
		});
	}
}
