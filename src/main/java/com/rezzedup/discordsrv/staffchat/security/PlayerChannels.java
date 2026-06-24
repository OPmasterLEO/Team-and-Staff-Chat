/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.security;

import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import pl.tlinkowski.annotation.basic.NullOr;

final class PlayerChannels {
	private PlayerChannels() {
	}

	static @NullOr Channel resolve(Player player) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object packetListener = handle.getClass().getField("connection").get(handle);
			Object networkManager = packetListener.getClass().getField("connection").get(packetListener);
			return (Channel) networkManager.getClass().getField("channel").get(networkManager);
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}
}
