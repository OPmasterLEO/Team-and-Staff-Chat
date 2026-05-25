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
package com.rezzedup.discordsrv.staffchat.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;
import pl.tlinkowski.annotation.basic.NullOr;

public class Strings {
	private Strings() {
	}
	
	private static final Pattern HASH_HEX_COLOR_PATTERN = Pattern.compile("(?i)&x?#(?<hex>[a-f0-9]{6})");
	
	public static String colorful(@NullOr String text) {
		if (isEmptyOrNull(text)) {
			return "";
		}
		
		Matcher matcher = HASH_HEX_COLOR_PATTERN.matcher(text);
		StringBuffer sb = new StringBuffer(text.length() + (text.length() >> 3));
		while (matcher.find()) {
			String hex = matcher.group("hex");
			StringBuilder bungeeHexFormat = new StringBuilder(14);
			bungeeHexFormat.append("&x");
			for (int i = 0; i < hex.length(); i++) {
				bungeeHexFormat.append('&').append(hex.charAt(i));
			}
			matcher.appendReplacement(sb, Matcher.quoteReplacement(bungeeHexFormat.toString()));
		}
		matcher.appendTail(sb);
		
		return ChatColor.translateAlternateColorCodes('&', sb.toString());
	}
	
	public static boolean isEmptyOrNull(@NullOr String text) {
		return text == null || text.isEmpty();
	}
	
	public static String orEmpty(@NullOr String text) {
		return (isEmptyOrNull(text)) ? "" : text;
	}
}
