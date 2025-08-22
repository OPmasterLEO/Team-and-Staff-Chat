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
package com.rezzedup.discordsrv.staffchat.config;

import com.github.zafarkhaja.semver.Version;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.Updater;
import com.rezzedup.discordsrv.staffchat.util.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.util.Strings;
import com.rezzedup.util.constants.Aggregates;
import com.rezzedup.util.constants.annotations.AggregatedResult;
import community.leaf.configvalues.bukkit.DefaultYamlValue;
import community.leaf.configvalues.bukkit.ExampleYamlValue;
import community.leaf.configvalues.bukkit.YamlValue;
import community.leaf.configvalues.bukkit.data.Load;
import community.leaf.configvalues.bukkit.data.YamlDataFile;
import community.leaf.configvalues.bukkit.migrations.Migration;
import community.leaf.configvalues.bukkit.util.Sections;
import org.bukkit.entity.Player;
import pl.tlinkowski.annotation.basic.NullOr;

import java.util.List;
import java.util.function.Predicate;

public class MessagesConfig extends YamlDataFile {
    public static final YamlValue<Version> VERSION =
        YamlValue.of("meta.config-version", Configs.VERSION).maybe();
    
    // Staff Chat Messages
    public static final DefaultYamlValue<String> PREFIX =
        YamlValue.ofString("placeholders.prefix")
            .defaults("&d(&5&l&oStaff&d)");
            
    // Team Chat Messages
    public static final DefaultYamlValue<String> TEAM_PREFIX =
        YamlValue.ofString("placeholders.team-prefix")
            .defaults("&b(&3&l&oTeam&b)");
            
    public static final ExampleYamlValue<String> EXAMPLE_PLACEHOLDER =
        YamlValue.ofString("placeholders.example")
            .example("Define your own placeholders here!");
    
    // Staff Chat Formats
    public static final DefaultYamlValue<String> IN_GAME_PLAYER_FORMAT =
        YamlValue.ofString("messages.in-game-formats.player")
            .migrates(Migration.move("in-game-message-format"))
            .defaults("%prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> IN_GAME_DISCORD_FORMAT =
        YamlValue.ofString("messages.in-game-formats.discord")
            .migrates(Migration.move("discord-message-format"))
            .defaults("&9&ldiscord &f→ %prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> IN_GAME_CONSOLE_FORMAT =
        YamlValue.ofString("messages.in-game-formats.console")
            .defaults("%prefix% [CONSOLE]&7:&f %message%");
    
    public static final DefaultYamlValue<String> DISCORD_CONSOLE_FORMAT =
        YamlValue.ofString("messages.discord-formats.console")
            .defaults("**`CONSOLE:`** %message%");
            
    // Team Chat Formats
    public static final DefaultYamlValue<String> TEAM_IN_GAME_PLAYER_FORMAT =
        YamlValue.ofString("messages.team-in-game-formats.player")
            .defaults("%team-prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> TEAM_IN_GAME_DISCORD_FORMAT =
        YamlValue.ofString("messages.team-in-game-formats.discord")
            .defaults("&9&ldiscord &f→ %team-prefix% %name%&7:&f %message%");
    
    public static final DefaultYamlValue<String> TEAM_IN_GAME_CONSOLE_FORMAT =
        YamlValue.ofString("messages.team-in-game-formats.console")
            .defaults("%team-prefix% [CONSOLE]&7:&f %message%");
    
    public static final DefaultYamlValue<String> TEAM_DISCORD_CONSOLE_FORMAT =
        YamlValue.ofString("messages.team-discord-formats.console")
            .defaults("**`CONSOLE:`** %message%");
    
    // Staff Chat Notifications
    public static final DefaultYamlValue<String> AUTO_ENABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-staff-chat.enabled")
            .migrates(Migration.move("enable-staff-chat"))
            .defaults("%prefix% &2→&a &nEnabled&a automatic staff chat");
    
    public static final DefaultYamlValue<String> AUTO_DISABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-staff-chat.disabled")
            .migrates(Migration.move("disable-staff-chat"))
            .defaults("%prefix% &4→&c &nDisabled&c automatic staff chat");
            
    // Team Chat Notifications
    public static final DefaultYamlValue<String> AUTO_TEAM_ENABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-team-chat.enabled")
            .defaults("%team-prefix% &2→&a &nEnabled&a automatic team chat");
    
    public static final DefaultYamlValue<String> AUTO_TEAM_DISABLED_NOTIFICATION =
        YamlValue.ofString("notifications.automatic-team-chat.disabled")
            .defaults("%team-prefix% &4→&c &nDisabled&c automatic team chat");
    
    // Staff Chat Leave/Join Messages
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.leave.self")
            .defaults(
                "%prefix% &4→&c You &nleft&c the staff chat&r\n" +
                    "&8&oYou won't receive any staff chat messages"
            );
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.leave.others")
            .defaults("%prefix% &4→&c %player% &nleft&c the staff chat");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_NOTIFICATION_REMINDER =
        YamlValue.ofString("notifications.leave.reminder")
            .defaults("&8&o(Reminder: you left the staff chat)");
    
    public static final DefaultYamlValue<String> LEFT_CHAT_DISABLED_ERROR =
        YamlValue.ofString("notifications.leave.disabled")
            .defaults(
                "%prefix% &6→&e You cannot leave the staff chat\n" +
                    "&8&oLeaving the staff chat is currently disabled"
            );
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.join.self")
            .defaults(
                "%prefix% &2→&a You &njoined&a the staff chat&r\n" +
                    "&8&oYou will now receive staff chat messages again"
            );
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.join.others")
            .defaults("%prefix% &2→&a %player% &njoined&a the staff chat");
    
    public static final DefaultYamlValue<String> JOIN_CHAT_NOTIFICATION_REMINDER =
        YamlValue.ofString("notifications.join.reminder")
            .defaults("&8&o(Reminder: you joined the staff chat)");
    
    // Team Chat Leave/Join Messages
    public static final DefaultYamlValue<String> LEFT_TEAM_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.team-leave.self")
            .defaults(
                "%team-prefix% &4→&c You &nleft&c the team chat&r\n" +
                    "&8&oYou won't receive any team chat messages"
            );
    
    public static final DefaultYamlValue<String> LEFT_TEAM_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.team-leave.others")
            .defaults("%team-prefix% &4→&c %player% &nleft&c the team chat");
    
    public static final DefaultYamlValue<String> LEFT_TEAM_CHAT_NOTIFICATION_REMINDER =
        YamlValue.ofString("notifications.team-leave.reminder")
            .defaults("&8&o(Reminder: you left the team chat)");
    
    public static final DefaultYamlValue<String> LEFT_TEAM_CHAT_DISABLED_ERROR =
        YamlValue.ofString("notifications.team-leave.disabled")
            .defaults(
                "%team-prefix% &6→&e You cannot leave the team chat\n" +
                    "&8&oLeaving the team chat is currently disabled"
            );
    
    public static final DefaultYamlValue<String> JOIN_TEAM_CHAT_NOTIFICATION_SELF =
        YamlValue.ofString("notifications.team-join.self")
            .defaults(
                "%team-prefix% &2→&a You &njoined&a the team chat&r\n" +
                    "&8&oYou will now receive team chat messages again"
            );
    
    public static final DefaultYamlValue<String> JOIN_TEAM_CHAT_NOTIFICATION_OTHERS =
        YamlValue.ofString("notifications.team-join.others")
            .defaults("%team-prefix% &2→&a %player% &njoined&a the team chat");
    
    // Sound Notifications
    public static final DefaultYamlValue<String> MUTE_SOUNDS_NOTIFICATION =
        YamlValue.ofString("notifications.sounds.muted")
            .defaults(
                "%prefix% &4→&c You have &nmuted&c staff chat sounds"
            );
    
    public static final DefaultYamlValue<String> UNMUTE_SOUNDS_NOTIFICATION =
        YamlValue.ofString("notifications.sounds.unmuted")
            .defaults(
                "%prefix% &2→&a You have &nunmuted&a staff chat sounds"
            );
            
    public static final DefaultYamlValue<String> MUTE_TEAM_SOUNDS_NOTIFICATION =
        YamlValue.ofString("notifications.team-sounds.muted")
            .defaults(
                "%team-prefix% &4→&c You have &nmuted&c team chat sounds"
            );
    
    public static final DefaultYamlValue<String> UNMUTE_TEAM_SOUNDS_NOTIFICATION =
        YamlValue.ofString("notifications.team-sounds.unmuted")
            .defaults(
                "%team-prefix% &2→&a You have &nunmuted&a team chat sounds"
            );
    
    @AggregatedResult
    public static final List<YamlValue<?>> VALUES =
        Aggregates.fromThisClass().constantsOfType(YamlValue.type()).toList();
    
    private final StaffChatPlugin plugin;
    
    private @NullOr MappedPlaceholder definitions = null;
    
    public MessagesConfig(StaffChatPlugin plugin) {
        super(plugin.directory(), "messages.config.yml", Load.LATER);
        this.plugin = plugin;
        
        reloadsWith(() ->
        {
            if (isInvalid()) {
                Configs.couldNotLoad(plugin.getLogger(), getFilePath());
                plugin.debug(getClass()).log("Reload", () -> "Couldn't load: " + getInvalidReason());
                
                // Add default placeholders
                if (definitions == null) {
                    definitions = new MappedPlaceholder();
                    definitions.map("prefix").to(PREFIX::getDefaultValue);
                }
                
                return;
            }
            
            Version existing = get(VERSION).orElse(Configs.NO_VERSION);
            @SuppressWarnings("deprecation")
            boolean isOutdated = existing.lessThan(plugin.version());
            
            if (isOutdated) {
                plugin.debug(getClass()).log("Reload", () -> "Updating outdated config: " + existing);
                set(VERSION, plugin.version());
            }
            
            headerFromResource("messages.config.header.txt");
            defaultValues(VALUES);
            
            if (isUpdated()) {
                plugin.debug(getClass()).log("Reload", () -> "Saving updated config and backing up old config: v" + existing);
                backupThenSave(plugin.backups(), "v" + existing);
            }
            
            // Remove old placeholder definitions
            definitions = null;
            
            // Load defined placeholders
            Sections.get(data(), "placeholders").ifPresent(section ->
            {
                definitions = new MappedPlaceholder();
                
                for (String key : section.getKeys(false)) {
                    @NullOr String value = section.getString(key);
                    if (Strings.isEmptyOrNull(value)) {
                        continue;
                    }
                    definitions.map(key).to(() -> value);
                }
            });
        });
    }
    
    public MappedPlaceholder placeholders() {
        MappedPlaceholder placeholders = new MappedPlaceholder();
        if (definitions != null) {
            placeholders.inherit(definitions);
        }
        return placeholders;
    }
    
    public MappedPlaceholder placeholders(Player player) {
        MappedPlaceholder placeholders = placeholders();
        
        placeholders.map("user", "name", "username", "player", "sender").to(player::getName);
        placeholders.map("nickname", "displayname").to(() -> player.getName());
        
        return placeholders;
    }
    
    private void sendNotification(Player player, String message) {
        player.sendMessage(message);
        plugin.config().playNotificationSound(player);
    }
    
    private void sendTeamNotification(Player player, String message) {
        player.sendMessage(message);
        plugin.config().playTeamNotificationSound(player);
    }
    
    private void sendNotification(Player player, DefaultYamlValue<String> self, @NullOr DefaultYamlValue<String> others) {
        MappedPlaceholder placeholders = placeholders(player);
        sendNotification(player, Strings.colorful(placeholders.update(getOrDefault(self))));
        
        if (others == null) {
            return;
        }
        
        String notification = Strings.colorful(placeholders.update(getOrDefault(others)));
        plugin.getServer().getConsoleSender().sendMessage(notification);
        
        plugin.onlineStaffChatParticipants()
            .filter(Predicate.not(player::equals))
            .forEach(staff -> sendNotification(staff, notification));
    }
    
    private void sendTeamNotification(Player player, DefaultYamlValue<String> self, @NullOr DefaultYamlValue<String> others) {
        MappedPlaceholder placeholders = placeholders(player);
        sendTeamNotification(player, Strings.colorful(placeholders.update(getOrDefault(self))));
        
        if (others == null) {
            return;
        }
        
        String notification = Strings.colorful(placeholders.update(getOrDefault(others)));
        plugin.getServer().getConsoleSender().sendMessage(notification);
        
        plugin.onlineTeamChatParticipants()
            .filter(Predicate.not(player::equals))
            .forEach(team -> sendTeamNotification(team, notification));
    }
    
    // Staff chat notifications
    public void notifyAutoChatEnabled(Player enabler) {
        sendNotification(enabler, AUTO_ENABLED_NOTIFICATION, null);
    }
    
    public void notifyAutoChatDisabled(Player disabler) {
        sendNotification(disabler, AUTO_DISABLED_NOTIFICATION, null);
    }
    
    public void notifyLeaveChat(Player leaver, boolean notifyOthers) {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? LEFT_CHAT_NOTIFICATION_OTHERS : null;
        sendNotification(leaver, LEFT_CHAT_NOTIFICATION_SELF, others);
    }
    
    public void notifyLeavingChatIsDisabled(Player leaver) {
        sendNotification(leaver, LEFT_CHAT_DISABLED_ERROR, null);
    }
    
    public void notifyJoinChat(Player joiner, boolean notifyOthers) {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? JOIN_CHAT_NOTIFICATION_OTHERS : null;
        sendNotification(joiner, JOIN_CHAT_NOTIFICATION_SELF, others);
    }
    
    public void notifySoundsMuted(Player player) {
        sendNotification(player, MUTE_SOUNDS_NOTIFICATION, null);
    }
    
    public void notifySoundsUnmuted(Player player) {
        sendNotification(player, UNMUTE_SOUNDS_NOTIFICATION, null);
    }
    
    // Team chat notifications
    public void notifyAutoTeamChatEnabled(Player enabler) {
        sendTeamNotification(enabler, AUTO_TEAM_ENABLED_NOTIFICATION, null);
    }
    
    public void notifyAutoTeamChatDisabled(Player disabler) {
        sendTeamNotification(disabler, AUTO_TEAM_DISABLED_NOTIFICATION, null);
    }
    
    public void notifyLeaveTeamChat(Player leaver, boolean notifyOthers) {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? LEFT_TEAM_CHAT_NOTIFICATION_OTHERS : null;
        sendTeamNotification(leaver, LEFT_TEAM_CHAT_NOTIFICATION_SELF, others);
    }
    
    public void notifyLeavingTeamChatIsDisabled(Player leaver) {
        sendTeamNotification(leaver, LEFT_TEAM_CHAT_DISABLED_ERROR, null);
    }
    
    public void notifyJoinTeamChat(Player joiner, boolean notifyOthers) {
        @NullOr DefaultYamlValue<String> others = (notifyOthers) ? JOIN_TEAM_CHAT_NOTIFICATION_OTHERS : null;
        sendTeamNotification(joiner, JOIN_TEAM_CHAT_NOTIFICATION_SELF, others);
    }
    
    public void notifyTeamSoundsMuted(Player player) {
        sendTeamNotification(player, MUTE_TEAM_SOUNDS_NOTIFICATION, null);
    }
    
    public void notifyTeamSoundsUnmuted(Player player) {
        sendTeamNotification(player, UNMUTE_TEAM_SOUNDS_NOTIFICATION, null);
    }
    
    //
    //  Unconfigurable notifications
    //
    
    public void notifyUpdateAvailable(Player manager, Version version) {
        sendNotification(manager, Strings.colorful(
            "&9DiscordSRV-&lStaff&9-&lChat&6 →&e Update available: &f" +
                version + " &6&o(" + plugin.version() + ")&r\n" + "&9&o&n" + Updater.RESOURCE_PAGE
        ));
    }
}