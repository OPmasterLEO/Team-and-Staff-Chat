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
package com.rezzedup.discordsrv.staffchat.listeners;

import com.rezzedup.discordsrv.staffchat.Permissions;
import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import com.rezzedup.discordsrv.staffchat.config.StaffChatConfig;
import community.leaf.eventful.bukkit.ListenerOrder;
import community.leaf.eventful.bukkit.annotations.EventListener;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerPrefixedMessageListener implements Listener {
    private final StaffChatPlugin plugin;
    
    public PlayerPrefixedMessageListener(StaffChatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventListener(ListenerOrder.EARLY)
    public void onChatMessage(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        
        // Staff chat messages
        if (plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_ENABLED)) {
            String prefix = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_CHAT_IDENTIFIER);
            
            if (message.startsWith(prefix) && Permissions.ACCESS.allows(sender)) {
                String content = message.substring(prefix.length()).trim();
                
                plugin.debug(getClass()).log(event, () ->
                    "Prefixed staff-chat message from " + sender.getName() + ": " + content
                );
                
                event.setCancelled(true);
                
                // Handle this on the main thread next tick.
                plugin.sync().run(() -> plugin.submitMessageFromPlayer(sender, content));
            }
        }
        
        // Team chat messages
        if (plugin.config().getOrDefault(StaffChatConfig.PREFIXED_TEAM_CHAT_ENABLED)) {
            String prefix = plugin.config().getOrDefault(StaffChatConfig.PREFIXED_TEAM_CHAT_IDENTIFIER);
            
            if (message.startsWith(prefix) && Permissions.TEAM_ACCESS.allows(sender)) {
                String content = message.substring(prefix.length()).trim();
                
                plugin.debug(getClass()).log(event, () ->
                    "Prefixed team-chat message from " + sender.getName() + ": " + content
                );
                
                event.setCancelled(true);
                
                // Handle this on the main thread next tick.
                plugin.sync().run(() -> plugin.submitTeamMessageFromPlayer(sender, content));
            }
        }
    }
}