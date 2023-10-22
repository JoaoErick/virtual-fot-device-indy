package com.device.fot.virtual.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.device.fot.virtual.model.FoTDevice;
import com.device.fot.virtual.util.CalculateScore;

public class ScoreLogController extends PersistenceController<Integer> {

    private final int COLLECTION_TIME = 2000; // Tempo entre os cálculos de escore

    private static ScoreLogController scoreLogController = new ScoreLogController();

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");

    private FoTDevice device = null;

    private ScoreLogController() {
        super("score_log.csv");
    }

    public synchronized static ScoreLogController getInstance() {
        return scoreLogController;
    }

    public void init(FoTDevice device) throws InterruptedException {
        if (canSaveData) {
            this.bufferSize = 16;
            this.device = device;
        }
    }

    public void putScore() throws InterruptedException {
        if (canSaveData) {
            int score = CalculateScore.calculateDeviceScore(this.device.getFoTSensors());
            buffer.put(score);
        }
    }

    private String buildLogScoreLine(int score) {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedTime = currentTime.format(formatter);
        return String.format("%s, score: %d", formattedTime, score);
    }

    @Override
    public void run() {
        running = true;
        var latencyLines = new ArrayList<String>(bufferSize);
        while (running) {
            try {
                if (this.device != null) {
                    putScore();
                }
                if (!buffer.isEmpty()) {
                    latencyLines.add(this.buildLogScoreLine(buffer.take()));
                    if (latencyLines.size() >= bufferSize) {
                        this.write_append(latencyLines);
                        latencyLines.clear();
                    }
                    Thread.sleep(COLLECTION_TIME);
                }
            } catch (InterruptedException ex) {
                this.write_append(latencyLines);
                this.running = false;
            }
        }
    }

    @Override
    public String getThreadName() {
        return "SCORE_LOG_WRITER";
    }
    
}
