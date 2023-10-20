package com.device.fot.virtual.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class LatencyLogController extends PersistenceController<Long> {

    private static LatencyLogController latencyLogController = new LatencyLogController();

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");

    private LatencyLogController() {
        super("latency_log.csv");
    }

    public synchronized static LatencyLogController getInstance() {
        return latencyLogController;
    }

    public void putLatency(Long latency) throws InterruptedException {
        if (canSaveData) {
            buffer.put(latency);
        }
    }

    private String buildLogLatencyLine(Long latency) {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedTime = currentTime.format(formatter);
        return String.format("%s,%d", formattedTime, latency);
    }

    @Override
    public void run() {
        running = true;
        var latencyLines = new ArrayList<String>(bufferSize);
        while (running) {
            try {
                if (!buffer.isEmpty()) {
                    latencyLines.add(this.buildLogLatencyLine(buffer.take()));
                    if (latencyLines.size() >= bufferSize) {
                        this.write(latencyLines);
                        latencyLines.clear();
                    }
                }
            } catch (InterruptedException ex) {
                this.write(latencyLines);
                this.running = false;
            }
        }
    }

    @Override
    public String getThreadName() {
        return "LATENCY_LOG_WRITER";
    }
}
