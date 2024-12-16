package com.dash0.homeexercise.service;

import java.util.Collection;
import java.util.Map;

import com.dash0.homeexercise.model.AttributeValueLogEntry;

import io.opentelemetry.proto.common.v1.AnyValue;

public interface LogAttributeCounterService {

	void report(final Collection<AttributeValueLogEntry> attributeValues);

	Map<AnyValue, Integer> observeAndReset();
}
