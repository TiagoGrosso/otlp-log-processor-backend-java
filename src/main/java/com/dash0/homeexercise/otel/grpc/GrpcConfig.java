// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise.otel.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "otel.grpc")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrpcConfig {

	@Min(1025)
	@Max(65536)
	private final int listenPort;

	@Min(4 * 1024 * 1024)
	@Max(128 * 1024 * 1024)
	private final int maxInboundMessageSize;

	@JsonCreator
	public GrpcConfig(
			@JsonProperty("listenPort") @NotBlank Integer listenPort,
			@JsonProperty("maxInboundMessageSize") @NotBlank Integer maxInboundMessageSize) {
		this.listenPort = listenPort;
		this.maxInboundMessageSize = maxInboundMessageSize;
	}

	public int getListenPort() {
		return listenPort;
	}

	public int getMaxInboundMessageSize() {
		return maxInboundMessageSize;
	}
}
