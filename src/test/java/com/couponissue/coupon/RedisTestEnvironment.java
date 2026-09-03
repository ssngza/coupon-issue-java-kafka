package com.couponissue.coupon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

final class RedisTestEnvironment implements AutoCloseable {

    private static final String IMAGE = "redis:7.4-alpine";
    private static final String FALLBACK_CONTAINER_NAME = "coupon-issue-redis-test";
    private static final int FALLBACK_PORT = 6380;

    private final String host;
    private final int port;
    private final AutoCloseable cleanup;

    private RedisTestEnvironment(String host, int port, AutoCloseable cleanup) {
        this.host = host;
        this.port = port;
        this.cleanup = cleanup;
    }

    static RedisTestEnvironment start() {
        try {
            return startWithTestcontainers();
        } catch (Throwable ignored) {
            return startWithDockerCli();
        }
    }

    String host() {
        return host;
    }

    int port() {
        return port;
    }

    @Override
    public void close() throws Exception {
        cleanup.close();
    }

    private static RedisTestEnvironment startWithTestcontainers() {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withExposedPorts(6379);
        container.start();
        return new RedisTestEnvironment(
                container.getHost(),
                container.getMappedPort(6379),
                container::stop
        );
    }

    private static RedisTestEnvironment startWithDockerCli() {
        runDockerCommandAllowFailure(List.of("rm", "-f", FALLBACK_CONTAINER_NAME));
        runDockerCommand(List.of("run", "-d", "--name", FALLBACK_CONTAINER_NAME, "-p", FALLBACK_PORT + ":6379", IMAGE));
        waitUntilHealthy(FALLBACK_CONTAINER_NAME);
        return new RedisTestEnvironment("localhost", FALLBACK_PORT, () -> runDockerCommandAllowFailure(List.of("rm", "-f", FALLBACK_CONTAINER_NAME)));
    }

    private static void waitUntilHealthy(String containerName) {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                CommandResult result = runDockerCommand(List.of("exec", containerName, "redis-cli", "ping"));
                if (result.exitCode() == 0 && result.output().contains("PONG")) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // keep polling
            }

            try {
                Thread.sleep(250L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for Redis to become ready", interruptedException);
            }
        }
        throw new IllegalStateException("Redis container did not become ready in time");
    }

    private static CommandResult runDockerCommand(List<String> args) {
        List<String> command = new ArrayList<>(args.size() + 1);
        command.add("docker");
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (left, right) -> left + right + System.lineSeparator());
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Docker command failed: " + String.join(" ", command) + System.lineSeparator() + output);
            }
            return new CommandResult(exitCode, output);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to run docker command: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running docker command: " + String.join(" ", command), exception);
        }
    }

    private static void runDockerCommandAllowFailure(List<String> args) {
        try {
            runDockerCommand(args);
        } catch (RuntimeException ignored) {
            // best-effort cleanup
        }
    }

    private record CommandResult(int exitCode, String output) {
    }
}
