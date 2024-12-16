package com.dash0.homeexercise.model;

import java.util.Optional;

import com.dash0.homeexercise.util.AttributeUtils;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;

public record LogRecordContext(ScopeContext scope, LogRecord logRecord) {
	public Optional<AnyValue> getAttributeValue(String key) {
		return AttributeUtils.findAttributeValue(logRecord.getAttributesList(), key)
				.or(() -> scope.getAttributeValue(key));
	}
}
