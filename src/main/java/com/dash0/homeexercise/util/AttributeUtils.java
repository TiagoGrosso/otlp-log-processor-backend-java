package com.dash0.homeexercise.util;

import java.util.List;
import java.util.Optional;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;

public final class AttributeUtils {
	private AttributeUtils() {
	}

	public static Optional<AnyValue> findAttributeValue(final List<KeyValue> attributes, final String key) {
		return attributes.stream()
				.filter(attribute -> attribute.getKey().equals(key))
				.findFirst()
				.map(KeyValue::getValue);
	}
}
