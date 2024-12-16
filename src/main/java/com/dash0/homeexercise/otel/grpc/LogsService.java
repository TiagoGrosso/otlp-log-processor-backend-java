// SPDX-FileCopyrightText: Copyright 2023-2024 Dash0 Inc.

package com.dash0.homeexercise.otel.grpc;

import java.util.List;

import com.dash0.homeexercise.model.AttributeValueLogEntry;
import com.dash0.homeexercise.model.LogEntry;
import com.dash0.homeexercise.model.LogRecordContext;
import com.dash0.homeexercise.model.ScopeContext;
import com.dash0.homeexercise.service.LogAttributeCounterService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsPartialSuccess;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.collector.logs.v1.LogsServiceGrpc;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.ResourceLogs;

public class LogsService extends LogsServiceGrpc.LogsServiceImplBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogsService.class);

	private final LongCounter requestsCount;
	private final LongCounter logsCount;
	@Value("${grpc.logs.attribute-to-track}")
	private String attributeToTrack;
	@Autowired
	private LogAttributeCounterService logAttributeCounterService;

	@Autowired
	public LogsService(final Meter meter) {
		this.requestsCount = meter.counterBuilder("grpc.requests.received.count")
				.setDescription("Count of Export Log requests received")
				.setUnit("requests")
				.build();
		this.logsCount = meter.counterBuilder("grpc.logs.received.count")
				.setDescription("Count of Log Records received in export log requests")
				.setUnit("requests")
				.build();
	}

	@Override
	public void export(
			final ExportLogsServiceRequest request,
			final StreamObserver<ExportLogsServiceResponse> responseObserver
	) {
		LOGGER.debug("Received ExportLogsServiceRequest");
		LOGGER.trace("ExportLogsServiceRequest data: {}", request);
		try {
			final var attributeValues = extractAttributeValues(request.getResourceLogsList());
			logAttributeCounterService.report(attributeValues);

			// TODO these counters should have a boolean attribute for "success" and be reported with it set to "false" when the above code throws an exception
			this.requestsCount.add(1);
			// Each log record will have produced exactly one attribute value
			this.logsCount.add(attributeValues.size());
			responseObserver.onNext(ExportLogsServiceResponse.newBuilder().setPartialSuccess(
					ExportLogsPartialSuccess.newBuilder().build()).build());
			responseObserver.onCompleted();
		} catch (Exception exception) {
			LOGGER.error("Error processing ExportLogsServiceRequest", exception);
			this.requestsCount.add(1, Attributes.of(AttributeKey.booleanKey("success"), false));
			responseObserver.onError(Status.INTERNAL.withDescription("foo").asRuntimeException());
		}
	}

	private List<AttributeValueLogEntry> extractAttributeValues(final List<ResourceLogs> resourceLogsList) {
		return resourceLogsList.stream()
				.flatMap(resourceLogs -> resourceLogs.getScopeLogsList().stream()
						.flatMap(scopeLogs -> {
									final var scopeContext = new ScopeContext(resourceLogs.getResource(), scopeLogs.getScope());
									return scopeLogs.getLogRecordsList().stream()
											.map(logRecord -> new LogRecordContext(scopeContext, logRecord));
								}
						))
				.map(logRecordContext -> new AttributeValueLogEntry(
						logRecordContext.getAttributeValue(attributeToTrack).orElseGet(() -> AnyValue.newBuilder().build()),
						new LogEntry(logRecordContext.logRecord().getBody().getStringValue(),
								logRecordContext.logRecord().getTimeUnixNano()))
				).toList();
	}
}
