// SPDX-FileCopyrightText: Copyright 2023 Dash0 Inc.

import net.ltgt.gradle.errorprone.errorprone
import java.nio.charset.StandardCharsets

plugins {
	java

	alias(libs.plugins.springBoot)
	alias(libs.plugins.springDependencyManagement)

	// Build-configuration plugins
	alias(libs.plugins.errorprone)
	alias(libs.plugins.forbiddenapis)
	alias(libs.plugins.testRetry)
}

java {
	val dotJavaVersion = layout.projectDirectory.file(".java-version").asFile
	val javaVersion: JavaVersion = JavaVersion.toVersion(dotJavaVersion.readText(StandardCharsets.US_ASCII).trim())

	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion
	toolchain {
		languageVersion = JavaLanguageVersion.of(javaVersion.majorVersion)
	}
}

repositories {
	mavenCentral()
	mavenLocal()
}

dependencyManagement {
	imports {
		mavenBom(libs.grpcBom.map { s -> s.toString() }.get())
		mavenBom(libs.opentelemetryBom.map { s -> s.toString() }.get())
		mavenBom(libs.opentelemetryAlphaBom.map { s -> s.toString() }.get())
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	implementation("io.grpc:grpc-protobuf")
	implementation("io.grpc:grpc-stub")
	runtimeOnly("io.grpc:grpc-netty")

	implementation("io.opentelemetry:opentelemetry-api")
	implementation("io.opentelemetry:opentelemetry-sdk")
	implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
	implementation("io.opentelemetry:opentelemetry-exporter-otlp")
	implementation("io.opentelemetry:opentelemetry-exporter-prometheus")
	implementation(libs.opentelemetrySemconv)
	implementation(libs.opentelemetryProto)
	// micrometer-shim for exposing metrics via OpenTelemetry native
	implementation(libs.opentelemetryMicrometer)
	// Automatic detection of Resource attributes
	runtimeOnly(libs.opentelemetryResources)
	implementation(libs.logstashLogbackEncoder)

	runtimeOnly("io.netty:netty-tcnative-boringssl-static")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
	testImplementation(libs.opentelemetryApiEvents)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher") {
		because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
	}

	errorprone(libs.errorproneCore)

}

forbiddenApis {
	bundledSignatures = setOf("jdk-unsafe", "jdk-deprecated", "jdk-non-portable", "jdk-internal")
	failOnUnsupportedJava = false
	ignoreSignaturesOfMissingClasses = true
}

springBoot {
	buildInfo()
}

tasks.withType<JavaCompile>().configureEach {
	options.errorprone.disableWarningsInGeneratedCode.set(true)
	options.errorprone.disable("JUnit4SetUpNotRun")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
