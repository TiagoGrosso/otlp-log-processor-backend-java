package com.dash0.homeexercise.model;

import io.opentelemetry.proto.common.v1.AnyValue;

public record AttributeValueLogEntry(AnyValue attributeValue, LogEntry logEntry) {
}
