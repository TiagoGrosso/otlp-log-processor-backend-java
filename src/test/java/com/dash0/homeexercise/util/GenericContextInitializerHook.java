// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise.util;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.TestSocketUtils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.events.GlobalEventEmitterProvider;

public class GenericContextInitializerHook implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		resetGlobalOpenTelemetry();

		TestPropertyValues.of(
				"otel.grpc.listenPort=" + TestSocketUtils.findAvailableTcpPort()
		).applyTo(applicationContext.getEnvironment());

	}

	private void resetGlobalOpenTelemetry() {
		// Reset the global OpenTelemetry instances before each test
		GlobalOpenTelemetry.resetForTest();
		GlobalEventEmitterProvider.resetForTest();
	}

}
