package com.dash0.homeexercise.service.notifier;

import static com.dash0.homeexercise.util.TestConstants.RANDOM;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import com.dash0.homeexercise.service.LogAttributeCounterService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.proto.common.v1.AnyValue;

@ExtendWith(MockitoExtension.class)
class NotifierServiceTest {

	@Mock
	private LogAttributeCounterService logAttributeCounterService;

	@Mock
	private Notifier notifier;

	@InjectMocks
	private NotifierService notifierService;

	@Test
	void whenNotifying_itRetrievesValuesAndForwardsThemToNotifier() {
		// Given
		final var values = Map.of(
				AnyValue.newBuilder().setStringValue(UUID.randomUUID().toString()).build(),
				RANDOM.nextInt()
		);

		// When
		when(logAttributeCounterService.observeAndReset()).thenReturn(values);
		notifierService.execute();

		// Assert
		verify(notifier).notifyValues(values);
	}
}
