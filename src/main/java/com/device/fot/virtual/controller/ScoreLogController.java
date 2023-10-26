package com.device.fot.virtual.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

import com.device.fot.virtual.model.FoTDevice;
import com.device.fot.virtual.util.CalculateScore;

public class ScoreLogController extends PersistenceController<Integer> {

    private static ScoreLogController scoreLogController = new ScoreLogController();

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");

    private FoTDevice device = null;

    private ScoreLogController() {
        super("score_log.csv");
    }

    public synchronized static ScoreLogController getInstance() {
        return scoreLogController;
    }

    public void init(FoTDevice device) {
        if (canSaveData) {
            this.bufferSize = 16;
            this.device = device;
        }
    }

    public void putScore() {
        if (canSaveData) {
            int score = CalculateScore.calculateDeviceScore(this.device.getFoTSensors());
            
            try {
                buffer.put(score);
            } catch (InterruptedException ex) {
                System.out.println("Oops! Error when adding the score to the write buffer...");
            }
        }
    }

    private String buildLogScoreLine(int score) {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedTime = currentTime.format(formatter);
        String id = UUID.randomUUID().toString()
                        .replaceAll("-", "")
                        .substring(0, 6);

        return String.format(
            "%s | ID: %s | Score: %d", 
            formattedTime, 
            id, 
            score
        );
    }

    @Override
    public void run() {
        running = true;
        var latencyLines = new ArrayList<String>(bufferSize);
        while (running) {
            try {
                if (!buffer.isEmpty()) {
                    latencyLines.add(this.buildLogScoreLine(buffer.take()));
                    if (latencyLines.size() >= bufferSize) {
                        this.write_append(latencyLines);
                        latencyLines.clear();
                    }
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
