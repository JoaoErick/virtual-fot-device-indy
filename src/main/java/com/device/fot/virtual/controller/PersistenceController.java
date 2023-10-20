package com.device.fot.virtual.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Uellington Damasceno
 */
public abstract class PersistenceController<T> implements Runnable {

    protected int bufferSize = 64;
    protected boolean running = false, canSaveData = false;
    protected Thread thread;
    protected String fileName;
    protected LinkedBlockingQueue<T> buffer = new LinkedBlockingQueue<>();

    protected PersistenceController(String fileName) {
        this.fileName = fileName;
    }
    
    public abstract String getThreadName();
    
    public void start() {
        if (this.thread == null || !running) {
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.setName(this.getThreadName());
            this.thread.start();
        }
    }

    public void setCanSaveData(boolean canSaveData) {
        this.canSaveData = canSaveData;
    }

    public void createAndUpdateFileName(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        this.fileName = fileName;
    }

    protected void write(List<String> lines) {
        try (var w = Files.newBufferedWriter(Path.of(this.fileName), StandardOpenOption.WRITE)) {
            lines.forEach(line -> {
                try {
                    w.write(line);
                    w.newLine();
                } catch (IOException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop() {
        running = false;
    }
}
