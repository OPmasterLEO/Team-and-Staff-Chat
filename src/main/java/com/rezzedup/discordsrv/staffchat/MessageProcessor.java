/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.events.ConsoleStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.ConsoleTeamChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.DiscordStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.DiscordTeamChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerStaffChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.events.PlayerTeamChatMessageEvent;
import com.rezzedup.discordsrv.staffchat.util.DiscordRolePlaceholders;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.SecureMessageDelivery;
import com.rezzedup.discordsrv.staffchat.util.Strings;

import community.leaf.configvalues.bukkit.DefaultYamlValue;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.emoji.EmojiParser;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import pl.tlinkowski.annotation.basic.NullOr;

public class MessageProcessor {
	private final StaffChatPlugin plugin;

	MessageProcessor(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}

	private boolean hasPlaceholderAPI() {
		return plugin.isPlaceholderApiPresent();
	}

	private String parsePlaceholders(@NullOr Player player, String text) {
		return hasPlaceholderAPI() ? PlaceholderAPI.setPlaceholders(player, text) : text;
	}

	private static String sanitizeUserMessage(String message) {
		return message.replace('%', '\uFF05');
	}

	private void sendFormattedChatMessage(
		boolean teamChat,
		@NullOr Object author,
		DefaultYamlValue<String> format,
		MappedPlaceholder placeholders,
		DefaultYamlValue<String> leftChatReminder
	) {
		if (Strings.isEmptyOrNull(placeholders.get("message"))) {
			return;
		}

		String formatted = plugin.messages().getOrDefault(format);
		if (hasPlaceholderAPI()) {
			@NullOr Player player = (author instanceof Player) ? (Player) author : null;
			formatted = parsePlaceholders(player, formatted);
		}

		String content = Strings.colorful(placeholders.update(formatted));

		if (author instanceof Player player) {
			StaffChatProfile profile = plugin.data().getOrCreateProfile(player);
			boolean receives = teamChat ? profile.receivesTeamChatMessages() : profile.receivesStaffChatMessages();
			if (!receives) {
				String reminder = Strings.colorful(placeholders.update(
					plugin.messages().getOrDefault(leftChatReminder)
				));
				SecureMessageDelivery.send(player, content);
				SecureMessageDelivery.send(player, reminder);
				if (teamChat) {
					plugin.config().playTeamNotificationSound(player);
				} else {
					plugin.config().playNotificationSound(player);
				}
			}
		}

		List<Player> recipients = teamChat
			? plugin.getCachedTeamParticipants()
			: plugin.getCachedStaffParticipants();

		SecureMessageDelivery.sendToMany(recipients, content);
		for (Player recipient : recipients) {
			if (teamChat) {
				plugin.config().playTeamMessageSound(recipient);
			} else {
				plugin.config().playMessageSound(recipient);
			}
		}

		plugin.getServer().getConsoleSender().sendMessage(content);
	}

	private void sendToDiscord(String channel, Consumer<TextChannel> sender) {
		@NullOr TextChannel discordChannel = channel.equals(StaffChatPlugin.TEAM_CHANNEL)
			? plugin.getTeamDiscordChannelOrNull()
			: plugin.getDiscordChannelOrNull();

		if (discordChannel == null) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"Unable to send message to discord: " + channel + " => null"
			);
			return;
		}

		plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
			"Sending message to discord channel: " + channel + " => " + discordChannel
		);

		sender.accept(discordChannel);
	}

	public void processConsoleChat(String message) {
		Objects.requireNonNull(message, "message");
		plugin.debug(getClass()).logConsoleChatMessage(message);

		ConsoleStaffChatMessageEvent event =
			plugin.events().call(new ConsoleStaffChatMessageEvent(message));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, event, () -> "Cancelled or text is empty");
			return;
		}

		MappedPlaceholder placeholders = plugin.messages().placeholders();
		placeholders.map("message", "content", "text").to(() -> sanitizeUserMessage(event.getText()));

		sendFormattedChatMessage(
			false,
			null,
			MessagesConfig.IN_GAME_CONSOLE_FORMAT,
			placeholders,
			MessagesConfig.LEFT_CHAT_NOTIFICATION_REMINDER
		);

		if (plugin.isDiscordSrvHookEnabled()) {
			String discordMessage = placeholders.update(
				plugin.messages().getOrDefault(MessagesConfig.DISCORD_CONSOLE_FORMAT)
			);
			sendToDiscord(StaffChatPlugin.CHANNEL, channel -> DiscordUtil.queueMessage(channel, discordMessage, true));
		} else {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"DiscordSRV hook is not enabled, cannot send to discord"
			);
		}
	}

	public void processPlayerChat(Player author, String message) {
		Objects.requireNonNull(author, "author");
		Objects.requireNonNull(message, "message");

		if (Permissions.ACCESS.denies(author)) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"Rejected staff chat from " + author.getName() + ": missing permission"
			);
			return;
		}

		plugin.debug(getClass()).logPlayerChatMessage(author, message);

		PlayerStaffChatMessageEvent event =
			plugin.events().call(new PlayerStaffChatMessageEvent(author, message));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, event, () -> "Cancelled or text is empty");
			return;
		}

		MappedPlaceholder placeholders = plugin.messages().placeholders(author);
		placeholders.map("message", "content", "text").to(() -> sanitizeUserMessage(event.getText()));

		sendFormattedChatMessage(
			false,
			author,
			MessagesConfig.IN_GAME_PLAYER_FORMAT,
			placeholders,
			MessagesConfig.LEFT_CHAT_NOTIFICATION_REMINDER
		);

		if (plugin.isDiscordSrvHookEnabled()) {
			String sanitized = sanitizeUserMessage(message);
			sendToDiscord(StaffChatPlugin.CHANNEL, channel -> plugin.runAsync(() ->
				DiscordSRV.getPlugin().processChatMessage(author, sanitized, StaffChatPlugin.CHANNEL, false)
			));
		} else {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"DiscordSRV hook is not enabled, cannot send to discord"
			);
		}
	}

	public void processDiscordChat(User author, Message message) {
		Objects.requireNonNull(author, "author");
		Objects.requireNonNull(message, "message");

		plugin.debug(getClass()).logDiscordChatMessage(author, message);

		DiscordStaffChatMessageEvent event =
			plugin.events().call(new DiscordStaffChatMessageEvent(author, message, message.getContentStripped()));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.DISCORD, "Message", () -> "Cancelled or text is empty");
			return;
		}

		String text = EmojiParser.parseToAliases(event.getText());
		MappedPlaceholder placeholders = DiscordRolePlaceholders.forDiscordMessage(author, message, text);

		sendFormattedChatMessage(
			false,
			author,
			MessagesConfig.IN_GAME_DISCORD_FORMAT,
			placeholders,
			MessagesConfig.LEFT_CHAT_NOTIFICATION_REMINDER
		);
	}

	public void processConsoleTeamChat(String message) {
		Objects.requireNonNull(message, "message");
		plugin.debug(getClass()).logConsoleChatMessage(message);

		ConsoleTeamChatMessageEvent event =
			plugin.events().call(new ConsoleTeamChatMessageEvent(message));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, event, () -> "Cancelled or text is empty");
			return;
		}

		MappedPlaceholder placeholders = plugin.messages().placeholders();
		placeholders.map("message", "content", "text").to(() -> sanitizeUserMessage(event.getText()));

		sendFormattedChatMessage(
			true,
			null,
			MessagesConfig.TEAM_IN_GAME_CONSOLE_FORMAT,
			placeholders,
			MessagesConfig.LEFT_TEAM_CHAT_NOTIFICATION_REMINDER
		);

		if (plugin.isDiscordSrvHookEnabled()) {
			String discordMessage = placeholders.update(
				plugin.messages().getOrDefault(MessagesConfig.TEAM_DISCORD_CONSOLE_FORMAT)
			);
			sendToDiscord(StaffChatPlugin.TEAM_CHANNEL, channel -> DiscordUtil.queueMessage(channel, discordMessage, true));
		} else {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"DiscordSRV hook is not enabled, cannot send to discord"
			);
		}
	}

	public void processPlayerTeamChat(Player author, String message) {
		Objects.requireNonNull(author, "author");
		Objects.requireNonNull(message, "message");

		if (Permissions.TEAM_ACCESS.denies(author)) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"Rejected team chat from " + author.getName() + ": missing permission"
			);
			return;
		}

		plugin.debug(getClass()).logPlayerChatMessage(author, message);

		PlayerTeamChatMessageEvent event =
			plugin.events().call(new PlayerTeamChatMessageEvent(author, message));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, event, () -> "Cancelled or text is empty");
			return;
		}

		MappedPlaceholder placeholders = plugin.messages().placeholders(author);
		placeholders.map("message", "content", "text").to(() -> sanitizeUserMessage(event.getText()));

		sendFormattedChatMessage(
			true,
			author,
			MessagesConfig.TEAM_IN_GAME_PLAYER_FORMAT,
			placeholders,
			MessagesConfig.LEFT_TEAM_CHAT_NOTIFICATION_REMINDER
		);

		if (plugin.isDiscordSrvHookEnabled()) {
			String sanitized = sanitizeUserMessage(message);
			sendToDiscord(StaffChatPlugin.TEAM_CHANNEL, channel -> plugin.runAsync(() ->
				DiscordSRV.getPlugin().processChatMessage(author, sanitized, StaffChatPlugin.TEAM_CHANNEL, false)
			));
		} else {
			plugin.debug(getClass()).log(ChatService.MINECRAFT, "Message", () ->
				"DiscordSRV hook is not enabled, cannot send to discord"
			);
		}
	}

	public void processDiscordTeamChat(User author, Message message) {
		Objects.requireNonNull(author, "author");
		Objects.requireNonNull(message, "message");

		plugin.debug(getClass()).logDiscordChatMessage(author, message);

		DiscordTeamChatMessageEvent event =
			plugin.events().call(new DiscordTeamChatMessageEvent(author, message, message.getContentStripped()));

		if (event.isCancelled() || event.getText().isEmpty()) {
			plugin.debug(getClass()).log(ChatService.DISCORD, "Message", () -> "Cancelled or text is empty");
			return;
		}

		String text = EmojiParser.parseToAliases(event.getText());
		MappedPlaceholder placeholders = DiscordRolePlaceholders.forDiscordMessage(author, message, text);

		sendFormattedChatMessage(
			true,
			author,
			MessagesConfig.TEAM_IN_GAME_DISCORD_FORMAT,
			placeholders,
			MessagesConfig.LEFT_TEAM_CHAT_NOTIFICATION_REMINDER
		);
	}
}
