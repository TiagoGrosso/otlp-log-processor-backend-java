package com.dash0.homeexercise.model;

import java.util.Optional;

import com.dash0.homeexercise.util.AttributeUtils;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.resource.v1.Resource;

public record ScopeContext(Resource resource, InstrumentationScope scope) {
	public Optional<AnyValue> getAttributeValue(String key) {
		return AttributeUtils.findAttributeValue(scope.getAttributesList(), key)
				.or(() -> AttributeUtils.findAttributeValue(resource.getAttributesList(), key));
	}
}
