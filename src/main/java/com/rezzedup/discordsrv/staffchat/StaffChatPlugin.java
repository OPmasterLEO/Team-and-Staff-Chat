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

import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.commands.ManageStaffChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.ManageTeamChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.StaffChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.TeamChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.ToggleStaffChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.ToggleStaffChatSoundsCommand;
import com.rezzedup.discordsrv.staffchat.commands.ToggleTeamChatCommand;
import com.rezzedup.discordsrv.staffchat.commands.ToggleTeamChatSoundsCommand;
import com.rezzedup.discordsrv.staffchat.config.MessagesConfig;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordSrvLoadedLaterListener;
import com.rezzedup.discordsrv.staffchat.listeners.DiscordStaffChatListener;
import com.rezzedup.discordsrv.staffchat.listeners.JoinNotificationListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerPrefixedMessageListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerStaffChatToggleListener;
import com.rezzedup.discordsrv.staffchat.listeners.PlayerTeamChatToggleListener;
import com.rezzedup.discordsrv.staffchat.util.FileIO;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.eventful.bukkit.BukkitEventSource;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.tlinkowski.annotation.basic.NullOr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class StaffChatPlugin extends JavaPlugin implements BukkitEventSource, StaffChatAPI {
	// https://bstats.org/plugin/bukkit/DiscordSRV-Staff-Chat/11056
	public static final int BSTATS = 11056;
	
	public static final String CHANNEL = "staff-chat";
	public static final String TEAM_CHANNEL = "team-chat";
	
	public static final String DISCORDSRV = "DiscordSRV";
	
	private @NullOr Version version;
	private @NullOr Path pluginDirectoryPath;
	private @NullOr Path backupsDirectoryPath;
	private @NullOr Debugger debugger;
	private @NullOr StaffChatConfig config;
	private @NullOr MessagesConfig messages;
	private @NullOr Data data;
	private @NullOr Updater updater;
	private @NullOr MessageProcessor processor;
	private @NullOr DiscordStaffChatListener discordSrvHook;

	private String serverType;
	
	private static TaskScheduler scheduler;
	
	@Override
	public void onEnable() {
		@SuppressWarnings("deprecation")
		Version v = Version.valueOf(getDescription().getVersion());
		this.version = v;
		// Determine server type early (simple heuristic since PlatformDetector removed)
		serverType = getServer().getVersion().toLowerCase().contains("folia") ? "Folia" : "Bukkit";
		
		this.pluginDirectoryPath = getDataFolder().toPath();
		this.backupsDirectoryPath = pluginDirectoryPath.resolve("backups");
		
		this.debugger = new Debugger(this);
		
		debug(getClass()).header(() -> "Starting Plugin: " + this);
		debugger().schedulePluginStatus(getClass(), "Enable");
		
		// Initialize UniversalScheduler first so we can use it for scheduling
		scheduler = UniversalScheduler.getScheduler(this);
		getLogger().info("Running on " + serverType + " server. Configuring for compatibility.");
		
		this.config = new StaffChatConfig(this);
		this.messages = new MessagesConfig(this);
		
		loadConfigurationFiles();
		
		this.data = new Data(this);
		this.updater = new Updater(this);
		this.processor = new MessageProcessor(this);
		
		events().register(new JoinNotificationListener(this));
		events().register(new PlayerPrefixedMessageListener(this));
		events().register(new PlayerStaffChatToggleListener(this));
		events().register(new PlayerTeamChatToggleListener(this));
		
		// Staff chat commands
		command("staffchat", new StaffChatCommand(this));
		// Replace deprecated direct call when registering manage command
		command("managestaffchat", new ManageStaffChatCommand(this, v.toString()));
		command("togglestaffchatsounds", new ToggleStaffChatSoundsCommand(this));
		
		ToggleStaffChatCommand staffToggle = new ToggleStaffChatCommand(this);
		command("leavestaffchat", staffToggle);
		command("joinstaffchat", staffToggle);
		
		// Team chat commands
		command("teamchat", new TeamChatCommand(this));
		command("manageteamchat", new ManageTeamChatCommand(this));
		command("toggleteamchatsounds", new ToggleTeamChatSoundsCommand(this));
		
		ToggleTeamChatCommand teamToggle = new ToggleTeamChatCommand(this);
		command("leaveteamchat", teamToggle);
		command("jointeamchat", teamToggle);
		
		@NullOr Plugin discordSrv = getServer().getPluginManager().getPlugin(DISCORDSRV);
		
		if (discordSrv != null) {
			debug(getClass()).log("Enable", () -> "DiscordSRV is enabled");
			subscribeToDiscordSrv(discordSrv);
		} else {
			debug(getClass()).log("Enable", () -> "DiscordSRV is not enabled: continuing without discord support");
			
			getLogger().warning("DiscordSRV is not currently enabled (messages will NOT be sent to Discord).");
			getLogger().warning("Staff chat and team chat messages will still work in-game, however.");
			
			// Subscribe to DiscordSRV later because it somehow hasn't enabled yet.
			events().register(new DiscordSrvLoadedLaterListener(this));
		}
		
		startMetrics();
		
		// Display toggle message so that auto staff-chat users are aware that their chat is private again.
		// Useful when hot loading this plugin on a live server.
		onlineStaffChatParticipants()
			.filter(data()::isAutomaticStaffChatEnabled)
			.forEach(messages()::notifyAutoChatEnabled);
			
		// Same for team chat users
		onlineTeamChatParticipants()
			.filter(data()::isAutomaticTeamChatEnabled)
			.forEach(messages()::notifyAutoTeamChatEnabled);
	}
	
	@Override
	public void onDisable() {
		debug(getClass()).log("Disable", () -> "Disabling plugin...");
		
		data().end();
		updater().end();
		
		// Display toggle message so that auto staff-chat users are aware that their chat is public again.
		// Useful when selectively disabling this plugin on a live server.
		onlineStaffChatParticipants()
			.filter(data()::isAutomaticStaffChatEnabled)
			.forEach(messages()::notifyAutoChatDisabled);
			
		// Same for team chat users
		onlineTeamChatParticipants()
			.filter(data()::isAutomaticTeamChatEnabled)
			.forEach(messages()::notifyAutoTeamChatDisabled);
		
		if (isDiscordSrvHookEnabled()) {
			debug(getClass()).log("Disable", () -> "Unsubscribing from DiscordSRV API (hook is enabled)");
			
			try {
				DiscordSRV.api.unsubscribe(discordSrvHook);
			} catch (RuntimeException ignored) {
			} // Don't show a user-facing error if DiscordSRV is already unloaded.
		}
		
		debug(getClass()).header(() -> "Disabled Plugin: " + this);
	}
	
	private <T> T initialized(@NullOr T thing) {
		if (thing != null) {
			return thing;
		}
		throw new IllegalStateException("Not initialized yet");
	}
	
	@Override
	public Plugin plugin() {
		return this;
	}
	
	public Version version() {
		return initialized(version);
	}
	
	public Path directory() {
		return initialized(pluginDirectoryPath);
	}
	
	public Path backups() {
		return initialized(backupsDirectoryPath);
	}
	
	public Debugger debugger() {
		return initialized(debugger);
	}
	
	public Debugger.DebugLogger debug(Class<?> clazz) {
		return debugger().debug(clazz);
	}
	
	public StaffChatConfig config() {
		return initialized(config);
	}
	
	public MessagesConfig messages() {
		return initialized(messages);
	}
	
	@Override
	public Data data() {
		return initialized(data);
	}
	
	public Updater updater() {
		return initialized(updater);
	}
	
	@Override
	public boolean isDiscordSrvHookEnabled() {
		return discordSrvHook != null;
	}
	
	public void subscribeToDiscordSrv(Plugin plugin) {
		debug(getClass()).log("Subscribe", () -> "Subscribing to DiscordSRV: " + plugin);
		
		if (!DISCORDSRV.equals(plugin.getName()) || !(plugin instanceof DiscordSRV)) {
			throw debug(getClass()).failure("Subscribe", new IllegalArgumentException("Not DiscordSRV: " + plugin));
		}
		
		if (isDiscordSrvHookEnabled()) {
			throw debug(getClass()).failure("Subscribe", new IllegalStateException(
				"Already subscribed to DiscordSRV. Did the server reload? ... If so, don't do that!"
			));
		}
		
		DiscordSRV.api.subscribe(discordSrvHook = new DiscordStaffChatListener(this));
		
		getLogger().info("Subscribed to DiscordSRV: messages will be sent to Discord");
	}
	
	@Override
	public @NullOr TextChannel getDiscordChannelOrNull() {
		return (isDiscordSrvHookEnabled())
			? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL)
			: null;
	}
	
	@Override
	public @NullOr TextChannel getTeamDiscordChannelOrNull() {
		return (isDiscordSrvHookEnabled())
			? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(TEAM_CHANNEL)
			: null;
	}
	
	private MessageProcessor processor() {
		return initialized(processor);
	}
	
	@Override
	public void submitMessageFromConsole(String message) {
		processor().processConsoleChat(message);
	}
	
	@Override
	public void submitMessageFromPlayer(Player author, String message) {
		processor().processPlayerChat(author, message);
	}
	
	@Override
	public void submitMessageFromDiscord(User author, Message message) {
		processor().processDiscordChat(author, message);
	}
	
	@Override
	public void submitTeamMessageFromConsole(String message) {
		processor().processConsoleTeamChat(message);
	}
	
	@Override
	public void submitTeamMessageFromPlayer(Player author, String message) {
		processor().processPlayerTeamChat(author, message);
	}
	
	@Override
	public void submitTeamMessageFromDiscord(User author, Message message) {
		processor().processDiscordTeamChat(author, message);
	}
	
	//
	//
	//
	
	private void loadConfigurationFiles() {
		// Explicitly load configs
		config().reload();
		messages().reload();
		
		// Upgrade & migrate legacy config if it exists
		upgradeLegacyConfig();
	}
	
	private void upgradeLegacyConfig(YamlDataFile file, List<YamlValue<?>> values) {
		file.migrateValues(values, getConfig());
		
		if (file.isNewlyCreated()) {
			file.save();
		} else {
			file.backupThenSave(backups(), "migrated");
		}
	}
	
	private void upgradeLegacyConfig() {
		Path legacyConfigPath = directory().resolve("config.yml");
		if (!Files.isRegularFile(legacyConfigPath)) {
			return;
		}
		
		debug(getClass()).log("Upgrade Legacy Config", () ->
			"Found legacy config, upgrading it to new configs..."
		);
		
		upgradeLegacyConfig(config(), StaffChatConfig.VALUES);
		upgradeLegacyConfig(messages(), MessagesConfig.VALUES);
		
		try {
			FileIO.backup(legacyConfigPath, backups().resolve("config.legacy.yml"));
			Files.deleteIfExists(legacyConfigPath);
		} catch (Exception e) {
			e.printStackTrace();
			debug(getClass()).log("Upgrade Legacy Config", () ->
				"Failed to backup legacy config: " + e.getMessage()
			);
		}
	}
	
	private void command(String name, CommandExecutor executor) {
		@NullOr PluginCommand command = getCommand(name);
		
		if (command == null) {
			debug(getClass()).log("Command: Setup", () ->
				"Unable to register command /" + name + " because it is not defined in plugin.yml"
			);
			return;
		}
		
		command.setExecutor(executor);
		debug(getClass()).log("Command: Setup", () -> "Registered command executor for: /" + name);
		
		if (executor instanceof TabCompleter) {
			command.setTabCompleter((TabCompleter) executor);
			debug(getClass()).log("Command: Setup", () -> "Registered tab completer for: /" + name);
		}
	}

	/* ------------------------------------------------------------------
	 *  bStats metrics
	 * ------------------------------------------------------------------ */

	private void startMetrics() {
		if (!config().getOrDefault(StaffChatConfig.METRICS_ENABLED)) {
			debug(getClass()).log("Metrics", () -> "Aborting: metrics are disabled in the config");
			return;
		}

		debug(getClass()).log("Metrics", () -> "Scheduling metrics to start one minute from now");

		// Start a minute later to get the most accurate data.
		getScheduler().runTaskLater(() -> {
			Metrics metrics = new Metrics(this, BSTATS);

			metrics.addCustomChart(new SimplePie(
					"hooked_into_discordsrv",
					() -> String.valueOf(isDiscordSrvHookEnabled())));

			metrics.addCustomChart(new SimplePie(
					"has_valid_staff-chat_channel",
					() -> String.valueOf(getDiscordChannelOrNull() != null)));

			metrics.addCustomChart(new SimplePie(
					"has_valid_team-chat_channel",
					() -> String.valueOf(getTeamDiscordChannelOrNull() != null)));

			debug(getClass()).log("Metrics", () -> "Started bStats metrics");
		}, 1200L); // 1 minute = 1200 ticks
	}
	
	/**
	 * Gets the UniversalScheduler task scheduler
	 * @return The task scheduler
	 */
	public static TaskScheduler getScheduler() {
		return scheduler;
	}
	
	/* ------------------------------------------------------------------
	 *  TaskSource helpers for compatibility with existing code
	 * ------------------------------------------------------------------ */
	
	/**
	 * Creates a task builder for synchronous tasks using UniversalScheduler
	 * @return TaskBuilder instance
	 */
	public TaskBuilder sync() {
		return new TaskBuilder(false);
	}
	
	/**
	 * Creates a task builder for asynchronous tasks using UniversalScheduler
	 * @return TaskBuilder instance
	 */
	public TaskBuilder async() {
		return new TaskBuilder(true);
	}
	
	/**
	 * Task builder class that mimics the original API but uses UniversalScheduler
	 */
	public class TaskBuilder {
		private final boolean async;
		private long delayTicks = 0L;
		private long periodTicks = 0L;
		
		public TaskBuilder(boolean async) {
			this.async = async;
		}
		
		/**
		 * Set a delay before task execution
		 * @param value Amount of time units
		 * @return This builder instance
		 */
		public TaskBuilder delay(long value) {
			this.delayTicks = value;
			return this;
		}
		
		/**
		 * Set time unit to ticks (default)
		 * @return This builder instance
		 */
		public TaskBuilder ticks() {
			// Default unit is ticks, no conversion needed
			return this;
		}
		
		/**
		 * Set time unit to seconds (convert to ticks)
		 * @return This builder instance
		 */
		public TaskBuilder seconds() {
			delayTicks *= 20L;
			periodTicks *= 20L;
			return this;
		}
		
		/**
		 * Set time unit to minutes (convert to ticks)
		 * @return This builder instance
		 */
		public TaskBuilder minutes() {
			delayTicks *= 20L * 60L;
			periodTicks *= 20L * 60L;
			return this;
		}
		
		/**
		 * Set time unit to hours (convert to ticks)
		 * @return This builder instance
		 */
		public TaskBuilder hours() {
			delayTicks *= 20L * 60L * 60L;
			periodTicks *= 20L * 60L * 60L;
			return this;
		}
		
		/**
		 * Set task to repeat at regular intervals
		 * @param value Interval time
		 * @return This builder instance
		 */
		public TaskBuilder every(long value) {
			this.periodTicks = value;
			return this;
		}
		
		/**
		 * Run the task with the configured parameters
		 * @param runnable Task to run
		 */
		public void run(Runnable runnable) {
			if (async) {
				// Handle asynchronous tasks
				if (delayTicks > 0) {
					if (periodTicks > 0) {
						// Delayed repeating async task
						scheduler.runTaskTimerAsynchronously(runnable, delayTicks, periodTicks);
					} else {
						// Delayed async task
						scheduler.runTaskLaterAsynchronously(runnable, delayTicks);
					}
				} else {
					if (periodTicks > 0) {
						// Immediate repeating async task
						scheduler.runTaskTimerAsynchronously(runnable, 0, periodTicks);
					} else {
						// Immediate async task
						scheduler.runTaskAsynchronously(runnable);
					}
				}
			} else {
				// Handle synchronous tasks
				if (delayTicks > 0) {
					if (periodTicks > 0) {
						// Delayed repeating sync task
						scheduler.runTaskTimer(runnable, delayTicks, periodTicks);
					} else {
						// Delayed sync task
						scheduler.runTaskLater(runnable, delayTicks);
					}
				} else {
					if (periodTicks > 0) {
						// Immediate repeating sync task
						scheduler.runTaskTimer(runnable, 0, periodTicks);
					} else {
						// Immediate sync task
						scheduler.runTask(runnable);
					}
				}
			}
		}
		
		/**
		 * Run the task with the configured parameters and provide access to the task object
		 * @param consumer Consumer that receives the scheduled task
		 */
		public void run(Consumer<MyScheduledTask> consumer) {
			MyScheduledTask task;
			
			if (async) {
				// Handle asynchronous tasks
				if (delayTicks > 0) {
					if (periodTicks > 0) {
						// Delayed repeating async task
						task = scheduler.runTaskTimerAsynchronously(() -> {
							consumer.accept(null); // Can't pass the task to itself in constructor
						}, delayTicks, periodTicks);
					} else {
						// Delayed async task
						task = scheduler.runTaskLaterAsynchronously(() -> {
							consumer.accept(null);
						}, delayTicks);
					}
				} else {
					if (periodTicks > 0) {
						// Immediate repeating async task
						task = scheduler.runTaskTimerAsynchronously(() -> {
							consumer.accept(null);
						}, 0, periodTicks);
					} else {
						// Immediate async task
						task = scheduler.runTaskAsynchronously(() -> {
							consumer.accept(null);
						});
					}
				}
			} else {
				// Handle synchronous tasks
				if (delayTicks > 0) {
					if (periodTicks > 0) {
						// Delayed repeating sync task
						task = scheduler.runTaskTimer(() -> {
							consumer.accept(null);
						}, delayTicks, periodTicks);
					} else {
						// Delayed sync task
						task = scheduler.runTaskLater(() -> {
							consumer.accept(null);
						}, delayTicks);
					}
				} else {
					if (periodTicks > 0) {
						// Immediate repeating sync task
						task = scheduler.runTaskTimer(() -> {
							consumer.accept(null);
						}, 0, periodTicks);
					} else {
						// Immediate sync task
						task = scheduler.runTask(() -> {
							consumer.accept(null);
						});
					}
				}
			}
			
			// Run once more with the actual task
			consumer.accept(task);
		}

		public TaskBuilder submit(Runnable runnable) { // compatibility alias
			run(runnable);
			return this;
		}
	}

	// Avoid deprecated getDescription() here
	public String getVersion() {
		return (version != null) ? version.toString() : "unknown";
	}
}