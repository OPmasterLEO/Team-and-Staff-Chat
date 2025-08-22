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
package com.rezzedup.discordsrv.staffchat;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.events.AutoStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.AutoTeamChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingStaffChatToggleEvent;
import com.rezzedup.discordsrv.staffchat.events.ReceivingTeamChatToggleEvent;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.util.Sections;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Data extends YamlDataFile implements StaffChatData {
	private static final String PROFILES_PATH = "staff-chat.profiles";
	
	private final Map<UUID, Profile> profilesByUuid = new HashMap<>();
	
	private final StaffChatPlugin plugin;
	
	private @NullOr MyScheduledTask task = null;
	
	Data(StaffChatPlugin plugin) {
		super(plugin.directory().resolve("data"), "staff-chat.data.yml");
		this.plugin = plugin;
		
		// Load persistent toggles.
		if (plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
			Sections.get(data(), PROFILES_PATH).ifPresent(section ->
			{
				for (String key : section.getKeys(false)) {
					try {
						getOrCreateProfile(UUID.fromString(key));
					} catch (IllegalArgumentException ignored) {
					}
				}
			});
		}
		
		// Start the save task (run sync every 2 minutes; async repeating caused UnsupportedOperationException on this server).
		long periodTicks = 2L * 60L * 20L;
		task = StaffChatPlugin.getScheduler().runTaskTimer(() -> {
			if (isUpdated()) {
				save();
			}
		}, periodTicks, periodTicks);

		
		// Update profiles of all online players when reloaded.
		reloadsWith(() -> plugin.getServer().getOnlinePlayers().forEach(this::updateProfile));
	}
	
	protected void end() {
		if (task != null && !task.isCancelled()) {
			try { task.cancel(); } catch (Throwable ignored) {}
		}
		if (isUpdated()) {
			save();
		}
	}
	
	@Override
	public StaffChatProfile getOrCreateProfile(UUID uuid) {
		return profilesByUuid.computeIfAbsent(uuid, k -> new Profile(plugin, this, k));
	}
	
	@Override
	public Optional<StaffChatProfile> getProfile(UUID uuid) {
		return Optional.ofNullable(profilesByUuid.get(uuid));
	}
	
	@Override
	public void updateProfile(Player player) {
		@NullOr Profile profile = profilesByUuid.get(player.getUniqueId());
		boolean isStaffMember = Permissions.ACCESS.allows(player);
		boolean isTeamMember = Permissions.TEAM_ACCESS.allows(player);
		
		if (isStaffMember || isTeamMember) {
			// Ensure that this staff/team member has an active profile.
			if (profile == null) {
				profile = (Profile) getOrCreateProfile(player);
			}
			
			// If leaving the staff chat is disabled...
			if (!plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED)) {
				// ... and this staff member previously left the staff chat ...
				if (isStaffMember && profile.left != null) {
					// Bring them back.
					profile.receivesStaffChatMessages(true);
				}
			}
			
			// If leaving the team chat is disabled...
			if (!plugin.config().getOrDefault(StaffChatConfig.LEAVING_TEAMCHAT_ENABLED)) {
				// ... and this team member previously left the team chat ...
				if (isTeamMember && profile.teamLeft != null) {
					// Bring them back.
					profile.receivesTeamChatMessages(true);
				}
			}
		} else {
			// Not a staff/team member but has a loaded profile...
			if (profile != null) {
				// Notify that they're no longer talking in staff/team chat.
				if (profile.automaticStaffChat()) {
					profile.automaticStaffChat(false);
				}
				if (profile.automaticTeamChat()) {
					profile.automaticTeamChat(false);
				}
				
				// No longer staff/team, delete data.
				profile.clearStoredProfileData();
				
				// Remove from the map.
				profilesByUuid.remove(player.getUniqueId());
			}
		}
	}
	
	static class Profile implements StaffChatProfile {
		static final YamlValue<Instant> AUTO_TOGGLE_DATE = YamlValue.ofInstant("toggles.auto").maybe();
		static final YamlValue<Instant> LEFT_TOGGLE_DATE = YamlValue.ofInstant("toggles.left").maybe();
		static final YamlValue<Boolean> MUTED_SOUNDS_TOGGLE = YamlValue.ofBoolean("toggles.muted-sounds").maybe();
		
		// Team chat toggles
		static final YamlValue<Instant> TEAM_AUTO_TOGGLE_DATE = YamlValue.ofInstant("toggles.team-auto").maybe();
		static final YamlValue<Instant> TEAM_LEFT_TOGGLE_DATE = YamlValue.ofInstant("toggles.team-left").maybe();
		static final YamlValue<Boolean> TEAM_MUTED_SOUNDS_TOGGLE = YamlValue.ofBoolean("toggles.team-muted-sounds").maybe();
		
		private final StaffChatPlugin plugin;
		private final YamlDataFile yaml;
		private final UUID uuid;
		
		private @NullOr Instant auto;
		private @NullOr Instant left;
		private boolean mutedSounds = false;
		
		// Team chat state
		private @NullOr Instant teamAuto;
		private @NullOr Instant teamLeft;
		private boolean teamMutedSounds = false;
		
		Profile(StaffChatPlugin plugin, YamlDataFile yaml, UUID uuid) {
			this.plugin = plugin;
			this.yaml = yaml;
			this.uuid = uuid;
			
			if (plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				Sections.get(yaml.data(), path()).ifPresent(section ->
				{
					// Staff chat toggles
					auto = AUTO_TOGGLE_DATE.get(section).orElse(null);
					left = LEFT_TOGGLE_DATE.get(section).orElse(null);
					mutedSounds = MUTED_SOUNDS_TOGGLE.get(section).orElse(false);
					
					// Team chat toggles
					teamAuto = TEAM_AUTO_TOGGLE_DATE.get(section).orElse(null);
					teamLeft = TEAM_LEFT_TOGGLE_DATE.get(section).orElse(null);
					teamMutedSounds = TEAM_MUTED_SOUNDS_TOGGLE.get(section).orElse(false);
				});
			}
		}
		
		String path() {
			return PROFILES_PATH + "." + uuid;
		}
		
		@Override
		public UUID uuid() {
			return uuid;
		}
		
		// Staff chat methods
		
		@Override
		public Optional<Instant> sinceEnabledAutoChat() {
			return Optional.ofNullable(auto);
		}
		
		@Override
		public boolean automaticStaffChat() {
			return auto != null;
		}
		
		@Override
		public void automaticStaffChat(boolean enabled) {
			if (plugin.events().call(new AutoStaffChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			auto = (enabled) ? Instant.now() : null;
			updateStoredProfileData();
		}
		
		@Override
		public Optional<Instant> sinceLeftStaffChat() {
			return Optional.ofNullable(left);
		}
		
		@Override
		public boolean receivesStaffChatMessages() {
			// hasn't left the staff chat or leaving is disabled outright
			return left == null || !plugin.config().getOrDefault(StaffChatConfig.LEAVING_STAFFCHAT_ENABLED);
		}
		
		@Override
		public void receivesStaffChatMessages(boolean enabled) {
			if (plugin.events().call(new ReceivingStaffChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			left = (enabled) ? null : Instant.now();
			updateStoredProfileData();
		}
		
		@Override
		public boolean receivesStaffChatSounds() {
			return !mutedSounds;
		}
		
		@Override
		public void receivesStaffChatSounds(boolean enabled) {
			mutedSounds = !enabled;
			updateStoredProfileData();
		}
		
		// Team chat methods
		
		@Override
		public Optional<Instant> sinceEnabledAutoTeamChat() {
			return Optional.ofNullable(teamAuto);
		}
		
		@Override
		public boolean automaticTeamChat() {
			return teamAuto != null;
		}
		
		@Override
		public void automaticTeamChat(boolean enabled) {
			if (plugin.events().call(new AutoTeamChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			teamAuto = (enabled) ? Instant.now() : null;
			updateStoredProfileData();
		}
		
		@Override
		public Optional<Instant> sinceLeftTeamChat() {
			return Optional.ofNullable(teamLeft);
		}
		
		@Override
		public boolean receivesTeamChatMessages() {
			// hasn't left the team chat or leaving is disabled outright
			return teamLeft == null || !plugin.config().getOrDefault(StaffChatConfig.LEAVING_TEAMCHAT_ENABLED);
		}
		
		@Override
		public void receivesTeamChatMessages(boolean enabled) {
			if (plugin.events().call(new ReceivingTeamChatToggleEvent(this, enabled)).isCancelled()) {
				return;
			}
			
			teamLeft = (enabled) ? null : Instant.now();
			updateStoredProfileData();
		}
		
		@Override
		public boolean receivesTeamChatSounds() {
			return !teamMutedSounds;
		}
		
		@Override
		public void receivesTeamChatSounds(boolean enabled) {
			teamMutedSounds = !enabled;
		 updateStoredProfileData();
		}
		
		boolean hasDefaultSettings() {
			return auto == null && left == null && !mutedSounds
				&& teamAuto == null && teamLeft == null && !teamMutedSounds;
		}
		
		void clearStoredProfileData() {
			if (!plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				return;
			}
			
			yaml.data().set(path(), null);
			yaml.updated(true);
		}
		
		void updateStoredProfileData() {
			if (!plugin.config().getOrDefault(StaffChatConfig.PERSIST_TOGGLES)) {
				return;
			}
			
			if (hasDefaultSettings()) {
				clearStoredProfileData();
				return;
			}
			
			ConfigurationSection section = Sections.getOrCreate(yaml.data(), path());
			
			// Staff chat toggles
			AUTO_TOGGLE_DATE.set(section, auto);
			LEFT_TOGGLE_DATE.set(section, left);
			MUTED_SOUNDS_TOGGLE.set(section, mutedSounds);
			
			// Team chat toggles
			TEAM_AUTO_TOGGLE_DATE.set(section, teamAuto);
			TEAM_LEFT_TOGGLE_DATE.set(section, teamLeft);
			TEAM_MUTED_SOUNDS_TOGGLE.set(section, teamMutedSounds);
			
			yaml.updated(true);
		}
	}
}