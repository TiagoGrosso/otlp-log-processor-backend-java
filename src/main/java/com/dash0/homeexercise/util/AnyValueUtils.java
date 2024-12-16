package com.dash0.homeexercise.util;

import java.util.Locale;

import io.opentelemetry.proto.common.v1.AnyValue;

public final class AnyValueUtils {
	public static final String UNKNOWN = "unknown";

	private AnyValueUtils() {
	}

	public static String anyValueToString(final AnyValue anyValue) {
		if (anyValue.hasStringValue()) {
			return String.format(Locale.ROOT, "\"%s\"", anyValue.getStringValue());
		}
		if (anyValue.hasIntValue()) {
			return String.format(Locale.ROOT, "%s", anyValue.getIntValue());
		}
		// Could add all other type possibilities but feels like I'm missing a better, potentially obvious, way to do this
		// For the sake of time I'll not investigate that too deeply and just leave it like this
		return UNKNOWN;
	}
}
