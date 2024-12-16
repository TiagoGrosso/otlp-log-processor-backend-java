// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise;

import static com.dash0.homeexercise.util.AnyValueUtils.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.dash0.homeexercise.otel.grpc.GrpcConfig;
import com.dash0.homeexercise.otel.grpc.GrpcServer;
import com.dash0.homeexercise.service.notifier.StdoutNotifier;
import com.dash0.homeexercise.util.GenericContextInitializerHook;
import com.dash0.homeexercise.util.OtelTestConfiguration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(OtelTestConfiguration.class)
@ContextConfiguration(initializers = {GenericContextInitializerHook.class})
class OtlpLogProcessorApplicationTests {

	@Autowired
	OpenTelemetryExtension otelExtension;
	@Autowired
	GrpcServer grpcServer;
	@Autowired
	GrpcConfig grpcConfig;

	@SpyBean
	private StdoutNotifier stdoutNotifier;

	@Value("${grpc.logs.attribute-to-track}")
	private String attributeToTrack;

	@Value("${grpc.logs.window}")
	private Duration window;

	@Test
	void testLogsIngestion() {
		OtlpGrpcLogRecordExporter logExporter =
				OtlpGrpcLogRecordExporter.builder().setEndpoint("http://localhost:" + grpcConfig.getListenPort()).build();

		// Build the OpenTelemetry BatchLogRecordProcessor with the GrpcExporter
		BatchLogRecordProcessor logRecordProcessor = BatchLogRecordProcessor.builder(logExporter).build();

		// Add the logRecord processor to the default TracerSdkProvider
		SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder().addLogRecordProcessor(logRecordProcessor).build();

		try (OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setLoggerProvider(loggerProvider).build()) {

			// Create an OpenTelemetry Logger
			var logger = loggerProvider.get("opentel-example");

			// Create a few basic log records
			logger.logRecordBuilder()
					.setBody("first message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("second message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("third message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey("some-other-attribute"), "foo")
					.emit();
			logger.logRecordBuilder()
					.setBody("fourth message")
					.setSeverity(Severity.INFO)
					.setAttribute(AttributeKey.stringKey(attributeToTrack), "baz")
					.emit();

			CompletableResultCode flushResult = logRecordProcessor.forceFlush().join(5, TimeUnit.SECONDS);
			assertThat(flushResult.isSuccess() || !flushResult.isDone()).isTrue();
			assertThat(logExporter.flush().isSuccess()).isTrue();

			openTelemetrySdk.shutdown();
		}

		final var attributeValueMetricData = getMetricDataPoints("grpc.logs.attribute-values.gauge");
		assertThat(attributeValueMetricData)
				.hasSize(3)
				.satisfiesExactlyInAnyOrder(
						data -> assertThat(data.getAttributes().get(AttributeKey.longKey("\"baz\""))).isEqualTo(1),
						data -> assertThat(data.getAttributes().get(AttributeKey.longKey("\"foo\""))).isEqualTo(2),
						data -> assertThat(data.getAttributes().get(AttributeKey.longKey(UNKNOWN))).isEqualTo(1)
				);

		final var requestsMetricData = getMetricDataPoints("grpc.requests.received.count");
		assertThat(requestsMetricData)
				.hasSize(1)
				.satisfiesExactly(pointData -> assertThat(((LongPointData) pointData).getValue()).isEqualTo(1));

		final var logsMetricData = getMetricDataPoints("grpc.logs.received.count");
		assertThat(logsMetricData)
				.hasSize(1)
				.satisfiesExactly(pointData -> assertThat(((LongPointData) pointData).getValue()).isEqualTo(4));

		final var notifyCaptor = ArgumentCaptor.forClass(Map.class);
		Awaitility.await()
				.atMost(window.plus(Duration.ofSeconds(5)))
				.pollInterval(Duration.of(1, ChronoUnit.SECONDS))
				.untilAsserted(() -> {
					verify(stdoutNotifier, atLeastOnce()).notifyValues(notifyCaptor.capture());
					assertThat(notifyCaptor.getAllValues())
							.anySatisfy(map -> assertThat(map).hasSize(3));
				});
	}

	private List<? extends PointData> getMetricDataPoints(final String metricName) {
		return otelExtension.getMetrics()
				.stream()
				.filter(metricData -> metricName.equals(metricData.getName()))
				.map(MetricData::getData)
				.flatMap(data -> data.getPoints().stream())
				.toList();
	}
}
