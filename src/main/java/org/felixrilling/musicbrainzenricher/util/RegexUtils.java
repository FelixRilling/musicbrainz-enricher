package org.felixrilling.musicbrainzenricher.util;

import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;

@ThreadSafe
public final class RegexUtils {

	private RegexUtils() {
	}

	public static @NotNull Optional<String> maybeGroup(@NotNull Matcher matcher, @NotNull String groupName) {
		return matcher.matches() ? Optional.ofNullable(matcher.group(groupName)) : Optional.empty();
	}
}
