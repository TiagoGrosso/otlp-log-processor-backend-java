package com.dash0.homeexercise.service;

import static com.dash0.homeexercise.util.AnyValueUtils.anyValueToString;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.dash0.homeexercise.model.AttributeValueLogEntry;
import com.dash0.homeexercise.model.LogEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.proto.common.v1.AnyValue;

@Component
public class InMemoryLogAttributeCounterService implements LogAttributeCounterService {

	private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryLogAttributeCounterService.class);

	private final Map<AnyValue, Set<LogEntry>> attributeValuesFrequencyMap;

	/**
	 * Gauge to keep track of the current value of the logs in the map.
	 */
	private final ObservableLongGauge asyncGauge;

	@Autowired
	public InMemoryLogAttributeCounterService(final Meter meter) {
		this.attributeValuesFrequencyMap = new ConcurrentHashMap<>();
		this.asyncGauge = meter.gaugeBuilder("grpc.logs.attribute-values.gauge")
				.ofLongs()
				.setDescription("Gauge of attribute value counts in current window")
				.setUnit("logs")
				.buildWithCallback(observableLongMeasurement -> {
					for (final var entry : attributeValuesFrequencyMap.entrySet()) {
						final var count = (long) entry.getValue().size();
						observableLongMeasurement.record(count,
								Attributes.of(AttributeKey.longKey(anyValueToString(entry.getKey())), count));
					}
				});
	}

	@VisibleForTesting
	InMemoryLogAttributeCounterService(
			final Map<AnyValue, Set<LogEntry>> attributeValuesFrequencyMap,
			final ObservableLongGauge gauge
	) {
		this.attributeValuesFrequencyMap = attributeValuesFrequencyMap;
		this.asyncGauge = gauge;
	}

	@VisibleForTesting
	InMemoryLogAttributeCounterService(final ObservableLongGauge gauge) {
		this.attributeValuesFrequencyMap = new ConcurrentHashMap<>();
		this.asyncGauge = gauge;
	}

	@Override
	public void report(final Collection<AttributeValueLogEntry> attributeValues) {
		LOGGER.trace("Reporting attributeValues: {}", attributeValues);
		for (final var value : attributeValues) {
			this.attributeValuesFrequencyMap.computeIfAbsent(value.attributeValue(), (_ign) -> ConcurrentHashMap.newKeySet())
					.add(value.logEntry());
		}
	}

	@Override
	public synchronized Map<AnyValue, Integer> observeAndReset() {
		LOGGER.debug("Observing counter and resetting");
		final var result = observe();
		LOGGER.trace("Observed result: {}", result);
		this.attributeValuesFrequencyMap.clear();
		LOGGER.debug("Performed reset");
		return result;
	}

	private Map<AnyValue, Integer> observe() {
		return this.attributeValuesFrequencyMap.entrySet()
				.stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entry -> entry.getValue().size()
				));
	}
}
