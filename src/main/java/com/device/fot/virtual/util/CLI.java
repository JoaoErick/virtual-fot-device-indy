package com.device.fot.virtual.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Uellington Damasceno
 */
public class CLI {

    public static Optional<String> getDeviceId() {
        return Optional.ofNullable(System.getenv("DEVICE_ID"));
    }

    public static Optional<String> getBrokerIp() {
        return Optional.ofNullable(System.getenv("BROKER_IP"));
    }

    public static Optional<String> getPort() {
        return Optional.ofNullable(System.getenv("PORT"));
    }

    public static Optional<String> getPassword() {
        return Optional.ofNullable(System.getenv("PASSWORD"));
    }

    public static Optional<String> getUsername() {
        return Optional.ofNullable(System.getenv("USERNAME"));
    }

    public static Optional<String> getTimeout() {
        return Optional.ofNullable(System.getenv("TIMEOUT"));
    }

    public static Optional<String> getAgentIp() {
        return Optional.ofNullable(System.getenv("AGENT_IP"));
    }

    public static Optional<String> getAgentPort() {
        return Optional.ofNullable(System.getenv("AGENT_PORT"));
    }

    public static boolean hasParam(String arg) {
        return System.getenv(arg) != null;
    }
}