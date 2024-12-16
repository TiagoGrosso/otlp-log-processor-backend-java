package com.dash0.homeexercise.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;

class AttributeUtilsTest {

	@Test
	void whenFindingAttributeValue_whenKeyNotPresent_itReturnsEmpty() {
		// Given
		final var attributes = List.of(
				KeyValue.newBuilder().setKey(UUID.randomUUID().toString()).setValue(AnyValue.newBuilder().build()).build(),
				KeyValue.newBuilder().setKey(UUID.randomUUID().toString()).setValue(AnyValue.newBuilder().build()).build()
		);

		// When
		final var result = AttributeUtils.findAttributeValue(attributes, UUID.randomUUID().toString());

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void whenFindingAttributeValue_whenKeyIsPresent_itReturnsItsMatchingAttributeValue() {
		final var attribute = KeyValue.newBuilder()
				.setKey(UUID.randomUUID().toString())
				.setValue(AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()))
				.build();
		final var attributes = List.of(
				KeyValue.newBuilder().setKey(UUID.randomUUID().toString()).setValue(AnyValue.newBuilder().build()).build(),
				attribute,
				KeyValue.newBuilder().setKey(UUID.randomUUID().toString()).setValue(AnyValue.newBuilder().build()).build()
		);

		// When
		final var result = AttributeUtils.findAttributeValue(attributes, attribute.getKey());

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attribute.getValue());
	}
}
