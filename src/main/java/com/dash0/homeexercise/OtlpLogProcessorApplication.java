// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

package com.dash0.homeexercise;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Main class for the Spring Boot application.
 */
@SpringBootApplication
public class OtlpLogProcessorApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger("com.dash0.homeexercise.OtlpLogProcessor");

	@Bean
	public CountDownLatch closeLatch() {
		return new CountDownLatch(1);
	}

	public static void main(String[] args) throws InterruptedException {
		ConfigurableApplicationContext ctx = SpringApplication.run(OtlpLogProcessorApplication.class, args);

		final CountDownLatch closeLatch = ctx.getBean(CountDownLatch.class);
		Runtime.getRuntime().addShutdownHook(new Thread(closeLatch::countDown));
		closeLatch.await();
	}
}
