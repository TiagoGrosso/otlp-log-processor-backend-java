# OTLP Log Parser (Java)

## Introduction

Solution to the https://github.com/dash0hq/take-home-assignments/tree/main/otlp-log-processor-backend-java challenge.

## Usage

### Configure the application

The application needs two properties (or [env variable]) to be defined:

- grpc.logs.attribute-to-track [`ATTRIBUTE_TO_TRACK`] - the attribute to count across log records
- grpc.logs.window [`WINDOW`] - the (tumbling) window (in Java Duration format) for reporting the current counts

Either change their default values in `application.properties` or provide the respective environment variables.

### Build the application:
```shell
./gradlew assemble
```

### Run the application:
```shell
./gradlew bootRun
```

You can now send `ExportLogsServiceRequest` to `grpc:localhost:4137`.

Every time the window elapses, if any log records have been sent to the grpc endpoint you should
see a log from `StdoutNotifier` reporting on the count of the tracked attribute's values within that window.

### Run tests
```shell
./gradlew check
```
