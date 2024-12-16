package com.dash0.homeexercise.service.notifier;

import static com.dash0.homeexercise.util.TestConstants.RANDOM;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.dash0.homeexercise.util.AnyValueUtils;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.opentelemetry.proto.common.v1.AnyValue;

class StdoutNotifierTest {

	private final StdoutNotifier notifier = new StdoutNotifier();

	@Test
	void whenNotifying_itLogsFormatedValues() {
		// Given
		final var stringValue = UUID.randomUUID().toString();
		final var stringValueCount = RANDOM.nextInt();
		final var intValue = RANDOM.nextInt();
		final var intValueCount = RANDOM.nextInt();
		final var unknownValueCount = RANDOM.nextInt();
		final var values = Map.of(
				AnyValue.newBuilder().setStringValue(stringValue).build(),
				stringValueCount,
				AnyValue.newBuilder().build(),
				unknownValueCount,
				AnyValue.newBuilder().setIntValue(intValue).build(),
				intValueCount
		);

		final var interceptedLogger = (Logger) LoggerFactory.getLogger(StdoutNotifier.class);
		final var listAppender = new ListAppender<ILoggingEvent>();
		listAppender.start();
		interceptedLogger.addAppender(listAppender);

		// When
		notifier.notifyValues(values);

		// Assert
		assertThat(listAppender.list)
				.extracting(ILoggingEvent::getFormattedMessage)
				.flatMap(message -> Arrays.stream(message.split("\n")).toList())
				.anySatisfy(message -> assertThat(message).contains(
						String.format(Locale.ROOT, "- \"%s\" - %s", stringValue, stringValueCount)))
				.anySatisfy(
						message -> assertThat(message).contains(String.format(Locale.ROOT, "- %s - %s", intValue, intValueCount)))
				.anySatisfy(
						message -> assertThat(message).contains(
								String.format(Locale.ROOT, "- %s - %s", AnyValueUtils.UNKNOWN, unknownValueCount))
				);
	}

	@Test
	void whenNotifying_givenEmptyMap_doesNothing() {
		// Given
		final var interceptedLogger = (Logger) LoggerFactory.getLogger(StdoutNotifier.class);
		final var listAppender = new ListAppender<ILoggingEvent>();
		listAppender.start();
		interceptedLogger.addAppender(listAppender);

		// When
		notifier.notifyValues(Map.of());

		// Assert
		assertThat(listAppender.list).isEmpty();
	}
}
