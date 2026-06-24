/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.util;

import java.util.Collection;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class SecureMessageDelivery {
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

	private SecureMessageDelivery() {
	}

	public static void send(Player player, String legacyMessage) {
		if (Strings.isEmptyOrNull(legacyMessage)) {
			return;
		}
		player.sendMessage(toComponent(legacyMessage));
	}

	public static void sendToMany(Collection<? extends Player> recipients, String legacyMessage) {
		if (Strings.isEmptyOrNull(legacyMessage) || recipients.isEmpty()) {
			return;
		}
		Component component = toComponent(legacyMessage);
		for (Player recipient : recipients) {
			recipient.sendMessage(component);
		}
	}

	private static Component toComponent(String legacyMessage) {
		return LEGACY.deserialize(legacyMessage);
	}
}
