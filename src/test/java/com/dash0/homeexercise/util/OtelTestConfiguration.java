// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise.util;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

@TestConfiguration
public class OtelTestConfiguration {
	@RegisterExtension
	public static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

	@Primary
	@Bean(name = "openTelemetryTesting")
	public OpenTelemetry openTelemetry() {
		// Override the OpenTelemetry initialization to register an in-memory MetricReader we can use to verify metrics
		return otelTesting.getOpenTelemetry();
	}

	@Bean
	public OpenTelemetryExtension openTelemetryExtension() {
		return otelTesting;
	}

}
