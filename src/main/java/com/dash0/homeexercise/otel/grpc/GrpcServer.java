// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise.otel.grpc;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * The Grpc Server handling connections.
 */
public class GrpcServer {
	private static final int MIN_THREADS = 3;

	private final Logger logger = LoggerFactory.getLogger(GrpcServer.class);

	private final Server server;
	private final GrpcConfig config;
	private final ThreadPoolExecutor executor;

	public GrpcServer(GrpcConfig config,
			LogsService logsService) {
		this.config = config;

		var processors = Runtime.getRuntime().availableProcessors();
		if (processors < MIN_THREADS) {
			processors = MIN_THREADS;
		}
		final var threadFactory = new ThreadFactoryBuilder()
				.setDaemon(true)
				.setNameFormat("grpc-otel-executor-%d")
				.setUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception in thread {}", t.getName(), e))
				.build();
		this.executor = new ThreadPoolExecutor(processors / 2, processors, 60L, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(processors * 20), threadFactory);
		this.server = Grpc.newServerBuilderForPort(config.getListenPort(), InsecureServerCredentials.create())
				.executor(this.executor)
				.addService(logsService)
				.maxInboundMessageSize(config.getMaxInboundMessageSize())
				.build();
	}

	@PostConstruct
	private void init() throws IOException {
		logger.debug("Starting up gRPC server on port {}...", config.getListenPort());

		/* The port on which the server should run */
		server.start();
		logger.info("gRPC Server started, listening on port {}", config.getListenPort());
	}

	@PreDestroy
	private void shutdown() throws InterruptedException {
		logger.info("Shutting down gRPC Server...");
		if (server != null) {
			server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
		}
		if (executor != null) {
			executor.shutdown();
		}
	}

}
