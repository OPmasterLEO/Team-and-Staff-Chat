/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatText {
	private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

	private ChatText() {
	}

	public static String plain(Component component) {
		return PLAIN.serialize(component);
	}
}
