package com.dash0.homeexercise.service;

import static com.dash0.homeexercise.util.TestConstants.RANDOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.dash0.homeexercise.model.AttributeValueLogEntry;
import com.dash0.homeexercise.model.LogEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.proto.common.v1.AnyValue;

@ExtendWith(MockitoExtension.class)
class InMemoryLogAttributeCounterServiceTest {

	@Mock
	private ObservableLongGauge mockGauge;

	private static Set<AttributeValueLogEntry> getRandomEntries(
			final List<String> possibleAttributeValues,
			final List<String> possibleLogEntryMessages
	) {
		return IntStream.range(0, RANDOM.nextInt(2, 10))
				.mapToObj(_ign -> {
					final var selectedAttributeValue =
							possibleAttributeValues.get(RANDOM.nextInt(0, possibleAttributeValues.size() - 1));
					final var logEntryMessage =
							possibleLogEntryMessages.get(RANDOM.nextInt(0, possibleLogEntryMessages.size() - 1));
					return new AttributeValueLogEntry(AnyValue.newBuilder().setStringValue(selectedAttributeValue).build(),
							new LogEntry(logEntryMessage, 0));
				}).collect(Collectors.toSet());
	}

	@Test
	void whenObserved_itReturnsCurrentCountsAndResets() {
		// Given
		final var map = new HashMap<AnyValue, Set<LogEntry>>();

		final var attributeValue1 = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		map.put(attributeValue1, Set.of(new LogEntry("1", 0)));
		final var attributeValue2 = AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build();
		map.put(attributeValue2, Set.of(new LogEntry("1", 0), new LogEntry("2", 0)));
		final var service = new InMemoryLogAttributeCounterService(map, mockGauge);

		// When
		final var results = service.observeAndReset();

		// Assert
		assertThat(results).hasSize(2)
				.contains(entry(attributeValue1, 1), entry(attributeValue2, 2));
		assertThat(map).isEmpty();
	}

	@Test
	void whenReport_givenConcurrentCalls_itCorrectlyInsertsValues() throws InterruptedException {
		// Given
		final var randomAttributeValues = IntStream.range(0, 10)
				.mapToObj(_ign -> UUID.randomUUID().toString())
				.toList();
		final var randomLogEntryMessages = IntStream.range(0, 90)
				.mapToObj(_ign -> UUID.randomUUID().toString())
				.toList();
		final var dataset = IntStream.range(0, 1000)
				.mapToObj(_ign -> getRandomEntries(randomAttributeValues, randomLogEntryMessages))
				.toList();
		final var service = new InMemoryLogAttributeCounterService(mockGauge);

		// When
		final var countDownLatch = new CountDownLatch(dataset.size());
		try (final var executor = Executors.newFixedThreadPool(8)) {
			for (final var data : dataset) {
				executor.submit(() -> {
					service.report(data);
					countDownLatch.countDown();
				});
			}
		}
		final var finished = countDownLatch.await(5000, TimeUnit.MILLISECONDS);
		final var result = service.observeAndReset();

		// Assert
		assertTrue(finished);

		final var expected = dataset.stream()
				.flatMap(Set::stream)
				.collect(Collectors.groupingBy(
						AttributeValueLogEntry::attributeValue,
						Collectors.collectingAndThen(
								Collectors.mapping(AttributeValueLogEntry::logEntry, Collectors.toSet()),
								Set::size
						)
				));

		assertEquals(result, expected);
	}
}
