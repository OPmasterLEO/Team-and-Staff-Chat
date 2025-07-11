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

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import pl.tlinkowski.annotation.basic.NullOr;

public class DiscordStaffChatListener {
    private final StaffChatPlugin plugin;
    
    public DiscordStaffChatListener(StaffChatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        
        @NullOr TextChannel staffChannel = plugin.getDiscordChannelOrNull();
        @NullOr TextChannel teamChannel = plugin.getTeamDiscordChannelOrNull();
        
        if (staffChannel != null && event.getChannel().getId().equals(staffChannel.getId())) {
            plugin.debug(getClass()).log(event, () ->
                "Discord staff chat message by " + event.getMember() + " in " + event.getChannel()
            );
            
            plugin.submitMessageFromDiscord(event.getAuthor(), event.getMessage());
        } 
        else if (teamChannel != null && event.getChannel().getId().equals(teamChannel.getId())) {
            plugin.debug(getClass()).log(event, () ->
                "Discord team chat message by " + event.getMember() + " in " + event.getChannel()
            );
            
            plugin.submitTeamMessageFromDiscord(event.getAuthor(), event.getMessage());
        }
    }
}