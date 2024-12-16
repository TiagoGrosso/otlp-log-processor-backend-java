package com.dash0.homeexercise.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;

@ExtendWith(MockitoExtension.class)
class LogRecordContextTest {

	@Mock
	private ScopeContext scopeContext;

	@Test
	void whenGettingAttributeValue_givenAttributeKeyNotPresent_returnsEmpty() {
		// Given
		final var logRecord = LogRecord.newBuilder().build();
		final var logRecordContext = new LogRecordContext(scopeContext, logRecord);
		final var attributeKey = UUID.randomUUID().toString();
		when(scopeContext.getAttributeValue(attributeKey)).thenReturn(Optional.empty());

		// When
		final var result = logRecordContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void whenGettingAttributeValue_givenAttributeKeyPresentOnlyInScope_returnsScopeLevelAttributeValue() {
		// Given
		final var logRecord = LogRecord.newBuilder().build();
		final var logRecordContext = new LogRecordContext(scopeContext, logRecord);
		final var attributeKey = UUID.randomUUID().toString();
		final var attributeValue = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		when(scopeContext.getAttributeValue(attributeKey)).thenReturn(Optional.of(attributeValue));

		// When
		final var result = logRecordContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attributeValue);
	}

	@Test
	void whenGettingAttributeValue_givenAttributeKeyLogRecord_returnsLogRecordLevelAttributeValue() {
		// Given
		final var attributeKey = UUID.randomUUID().toString();
		final var attributeValue = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		final var logRecord = LogRecord.newBuilder()
				.addAttributes(
						KeyValue.newBuilder()
								.setKey(attributeKey)
								.setValue(attributeValue)
								.build()
				)
				.build();
		final var logRecordContext = new LogRecordContext(scopeContext, logRecord);

		// When
		final var result = logRecordContext.getAttributeValue(attributeKey);

		// Assert
		assertThat(result)
				.isPresent()
				.get()
				.isEqualTo(attributeValue);

		verifyNoInteractions(scopeContext);
	}
}
