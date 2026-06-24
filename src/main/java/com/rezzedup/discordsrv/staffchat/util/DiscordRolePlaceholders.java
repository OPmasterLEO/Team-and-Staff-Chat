/*
 * The MIT License
 * Copyright © 2017-2024 RezzedUp and Contributors
 */
package com.rezzedup.discordsrv.staffchat.util;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.md_5.bungee.api.ChatColor;
import pl.tlinkowski.annotation.basic.NullOr;

public final class DiscordRolePlaceholders {
	private DiscordRolePlaceholders() {
	}

	public static MappedPlaceholder forDiscordMessage(User author, Message message, String text) {
		MappedPlaceholder placeholders = new MappedPlaceholder();
		placeholders.map("message", "content", "text").to(() -> text);
		placeholders.map("user", "name", "username", "sender").to(author::getName);
		placeholders.map("discriminator", "discrim").to(author::getDiscriminator);

		@NullOr Member member = message.getMember();
		if (member != null) {
			placeholders.map("nickname", "displayname").to(member::getEffectiveName);
			applyRolePlaceholders(placeholders, member);
		}
		return placeholders;
	}

	private static void applyRolePlaceholders(MappedPlaceholder placeholders, Member member) {
		DiscordSRV discordSrv = DiscordSRV.getPlugin();
		List<Role> selectedRoles = discordSrv.getSelectedRoles(member);
		@NullOr Role topRole = selectedRoles.isEmpty() ? null : selectedRoles.get(0);
		if (topRole == null) {
			return;
		}

		placeholders.map("toprole").to(topRole::getName);
		placeholders.map("toproleinitial").to(() -> topRole.getName().substring(0, 1));
		placeholders.map("toprolealias").to(() ->
			discordSrv.getRoleAliases().getOrDefault(
				topRole.getId(),
				discordSrv.getRoleAliases().getOrDefault(
					topRole.getName().toLowerCase(Locale.ROOT),
					topRole.getName()
				)
			)
		);
		placeholders.map("toprolecolor").to(() -> ChatColor.of(new Color(topRole.getColorRaw())));
		placeholders.map("allroles").to(() -> DiscordUtil.getFormattedRoles(selectedRoles));
	}
}
