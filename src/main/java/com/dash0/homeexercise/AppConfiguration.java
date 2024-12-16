// SPDX-FileCopyrightText: Copyright 2023-2024 Dash0 Inc.

package com.dash0.homeexercise;

import com.dash0.homeexercise.otel.grpc.GrpcConfig;
import com.dash0.homeexercise.otel.grpc.GrpcServer;
import com.dash0.homeexercise.otel.grpc.LogsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.annotation.Validated;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.annotation.PostConstruct;

@Validated
@Configuration
@ConfigurationProperties(prefix = "dash0") // Annotation needed to make validation work on @Value fields
@ConfigurationPropertiesScan
@EnableScheduling
public class AppConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger("com.dash0.homeexercise.OtlpLogProcessor");

	@Autowired
	private Environment env;


	@PostConstruct
	private void init() {
	}


	/**
	 * Use Auto Configuration for OpenTelemetry SDK as bean.
	 */
	@Bean
	public OpenTelemetry openTelemetry() {
		// If the OpenTelemetry Agent is attached, the SDK is automatically initialized and we expect it to have
		// all the right configs already set.
		// When there's no Agent, manually initialize and configure the SDK
		if (agentIsAttached()) {
			return GlobalOpenTelemetry.get();
		} else {
			return AutoConfiguredOpenTelemetrySdk.builder().addResourceCustomizer((resource, config) -> {
				ResourceBuilder rb = resource.toBuilder();
				return rb.build();
			}).build().getOpenTelemetrySdk();
		}
	}

	private static boolean agentIsAttached() {
		try {
			Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Use the Micrometer-shim for OpenTelemetry, exposing all metrics in the OpenTelemetry Meter and allowing to export
	 * via native OLTP.
	 * <p/>
	 * <b>NOTE</b>: creating OpenTelemetry native metrics is preferred to prevent conversion which might
	 * lose information
	 *
	 * @return {@link MeterRegistry} instance for registering Micrometer metrics.
	 */
	@Bean
	public MeterRegistry meterRegistry(OpenTelemetry openTelemetry) {
		return OpenTelemetryMeterRegistry.builder(openTelemetry).build();
	}

	/**
	 * Enable @Timed annotation to be tracked.
	 *
	 * @return Initialized {@link TimedAspect} instance
	 */
	@Bean
	public TimedAspect timedAspect(MeterRegistry meterRegistry) {
		return new TimedAspect(meterRegistry);
	}

	@Bean
	public Meter meter(OpenTelemetry openTelemetry) {
		// Currently we need only a single 'namespace' for all instruments (counters, gauges etc)
		// Use this Meter to register all instruments.
		return openTelemetry.meterBuilder("com.dash0.homeexercise").build();
	}

	@Bean
	public GrpcServer grpcServer(GrpcConfig config, LogsService logsService) {
		return new GrpcServer(config, logsService);
	}

	@Bean
	public LogsService logsService(final Meter meter) {
		return new LogsService(meter);
	}

}
