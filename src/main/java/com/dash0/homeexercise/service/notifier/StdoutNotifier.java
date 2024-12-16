package com.dash0.homeexercise.service.notifier;

import static com.dash0.homeexercise.util.AnyValueUtils.anyValueToString;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.opentelemetry.proto.common.v1.AnyValue;

@Component
public class StdoutNotifier implements Notifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(StdoutNotifier.class);

	@Override
	public void notifyValues(final Map<AnyValue, Integer> data) {
		if (data.isEmpty()) {
			return;
		}

		final var message = data.entrySet()
				.stream()
				.map(entry -> String.format(Locale.ROOT, "- %s - %s", anyValueToString(entry.getKey()), entry.getValue()))
				.collect(Collectors.joining("\n"));
		LOGGER.info("\n{}", message);
	}
}
