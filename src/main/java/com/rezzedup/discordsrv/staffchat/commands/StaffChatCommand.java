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
package com.rezzedup.discordsrv.staffchat.commands;

import com.rezzedup.discordsrv.staffchat.StaffChatPlugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand {
	private final StaffChatPlugin plugin;
	
	public StaffChatCommand(StaffChatPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void register() {
		new CommandAPICommand("staffchat")
			.withAliases("adminchat", "schat", "achat", "sc", "ac", "a")
			.withPermission("staffchat.access")
			.withOptionalArguments(new GreedyStringArgument("message"))
			.executes((sender, args) -> {
				if (sender == null) {
					return;
				}
				String message = (String) args.get("message");
				if (message == null || message.isBlank()) {
					if (sender instanceof Player) {
						plugin.data().getOrCreateProfile((Player) sender).toggleAutomaticStaffChat();
					} else {
						sender.sendMessage("Only players may toggle automatic staff chat.");
					}
					return;
				}
				if (sender instanceof Player) {
					plugin.submitMessageFromPlayer((Player) sender, message);
				} else if (sender instanceof ConsoleCommandSender) {
					plugin.submitMessageFromConsole(message);
				} else {
					sender.sendMessage("Unsupported command sender type: " + sender.getClass().getSimpleName());
				}
			})
			.register();
	}
}
