package com.dash0.homeexercise.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;

class ScopeContextTest {

	@Test
	void whenGettingAttributeValue_givenAttributeKeyNotPresent_returnsEmpty() {
		// Given
		final var resource = Resource.newBuilder().build();
		final var scope = InstrumentationScope.newBuilder().build();
		final var scopeContext = new ScopeContext(resource, scope);

		// When
		final var result = scopeContext.getAttributeValue("some-attribute");

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void whenGettingAttributeValue_givenAttributeKeyPresentOnlyInResource_returnsResourceLevelAttributeValue() {
		// Given
		final var attributeKey = UUID.randomUUID().toString();
		final var attributeValue = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		final var resource = Resource.newBuilder()
				.addAttributes(
						KeyValue.newBuilder()
								.setKey(attributeKey)
								.setValue(attributeValue)
								.build()
				).build();
		final var scope = InstrumentationScope.newBuilder().build();
		final var scopeContext = new ScopeContext(resource, scope);

		// When
		final var result = scopeContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attributeValue);
	}

	@Test
	void whenGettingAttributeValue_givenAttributeKeyPresentOnlyInScope_returnsScopeLevelAttributeValue() {
		// Given
		final var attributeKey = UUID.randomUUID().toString();
		final var attributeValue = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		final var resource = Resource.newBuilder().build();
		final var scope = InstrumentationScope.newBuilder()
				.addAttributes(
						KeyValue.newBuilder()
								.setKey(attributeKey)
								.setValue(attributeValue)
								.build()
				).build();
		final var scopeContext = new ScopeContext(resource, scope);

		// When
		final var result = scopeContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attributeValue);
	}

	@Test
	void whenGettingAttributeValue_givenAttributeKeyPresentInResourceAndScope_returnsScopeLevelAttributeValue() {
		// Given
		final var attributeKey = UUID.randomUUID().toString();
		final var attributeValue = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		final var resource = Resource.newBuilder()
				.addAttributes(
						KeyValue.newBuilder()
								.setKey(attributeKey)
								.setValue(AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build())
								.build()
				).build();
		final var scope = InstrumentationScope.newBuilder()
				.addAttributes(
						KeyValue.newBuilder()
								.setKey(attributeKey)
								.setValue(attributeValue)
								.build()
				).build();
		final var scopeContext = new ScopeContext(resource, scope);

		// When
		final var result = scopeContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attributeValue);
	}
}
