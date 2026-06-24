/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.security;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.kyori.adventure.key.Key;
import pl.tlinkowski.annotation.basic.NullOr;

/**
 * Drops outgoing player-chat packets for players whose message was rerouted to
 * staff/team chat, even if another plugin uncancelled the chat event.
 */
public final class OutgoingChatPacketGuard {
	private static final String HANDLER_NAME = "discordsrv_staffchat_packet_guard";
	private static final ConcurrentHashMap<Channel, UUID> CHANNEL_OWNERS = new ConcurrentHashMap<>(128);

	private OutgoingChatPacketGuard() {
	}

	public static void register(StaffChatPlugin plugin) {
		if (!registerChannelListener()) {
			plugin.getLogger().info(
				"Netty packet guard unavailable on this server; chat leak protection uses event guards only."
			);
		}
		plugin.getServer().getPluginManager().registerEvents(new PlayerChannelBindingListener(plugin), plugin);
	}

	static void bindPlayer(Channel channel, UUID playerId) {
		CHANNEL_OWNERS.put(channel, playerId);
	}

	private static boolean registerChannelListener() {
		try {
			Class<?> holderClass = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
			Class<?> listenerClass = Class.forName("io.papermc.paper.network.ChannelInitializeListener");

			Object listener = Proxy.newProxyInstance(
				listenerClass.getClassLoader(),
				new Class<?>[] { listenerClass },
				(proxy, method, args) -> {
					if (args != null && args.length == 1) {
						installHandler((Channel) args[0]);
					}
					return null;
				}
			);

			holderClass
				.getMethod("addListener", Key.class, listenerClass)
				.invoke(null, Key.key("discordsrv_staffchat", "outgoing_guard"), listener);
			return true;
		} catch (ReflectiveOperationException | SecurityException ignored) {
			return false;
		}
	}

	private static void installHandler(Channel channel) {
		channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ChannelDuplexHandler() {
			@Override
			public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
				if (shouldDropOutgoingChatPacket(channel, msg)) {
					promise.setSuccess();
					return;
				}
				super.write(ctx, msg, promise);
			}

			@Override
			public void handlerRemoved(ChannelHandlerContext ctx) {
				CHANNEL_OWNERS.remove(channel);
			}
		});
	}

	private static boolean shouldDropOutgoingChatPacket(Channel channel, Object packet) {
		if (!isPlayerChatPacket(packet)) {
			return false;
		}
		@NullOr UUID owner = CHANNEL_OWNERS.get(channel);
		return owner != null && ChatInterceptionTracker.wasRecentlyIntercepted(owner);
	}

	private static boolean isPlayerChatPacket(Object packet) {
		String name = packet.getClass().getSimpleName();
		return name.contains("PlayerChat") || name.contains("DisguisedChat");
	}
}
