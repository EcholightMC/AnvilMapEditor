package net.minestom.server.utils.validate;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

public class Check {

	@Contract("true, _ -> fail")
	public static void argCondition(boolean condition, @NotNull String reason) {
		if (condition) {
			throw new IllegalArgumentException(reason);
		}
	}

	@Contract("true, _, _ -> fail")
	public static void argCondition(boolean condition, @NotNull String reason, Object... arguments) {
		if (condition) {
			throw new IllegalArgumentException(MessageFormat.format(reason, arguments));
		}
	}

}
