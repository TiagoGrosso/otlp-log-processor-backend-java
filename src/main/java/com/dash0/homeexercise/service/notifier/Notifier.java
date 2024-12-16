package com.dash0.homeexercise.service.notifier;

import java.util.Map;

import io.opentelemetry.proto.common.v1.AnyValue;

public interface Notifier {
	void notifyValues(final Map<AnyValue, Integer> data);
}
