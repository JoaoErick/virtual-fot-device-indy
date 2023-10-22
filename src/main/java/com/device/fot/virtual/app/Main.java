package com.device.fot.virtual.app;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import com.device.fot.virtual.controller.AriesController;
import com.device.fot.virtual.controller.BrokerUpdateCallback;
import com.device.fot.virtual.controller.LatencyLogController;
import com.device.fot.virtual.controller.MessageLogController;
import com.device.fot.virtual.controller.ScoreLogController;
import com.device.fot.virtual.model.BrokerSettings;
import com.device.fot.virtual.model.BrokerSettingsBuilder;
import com.device.fot.virtual.model.FoTDevice;
import com.device.fot.virtual.model.FoTSensor;
import com.device.fot.virtual.util.CLI;

import extended.tatu.wrapper.model.Sensor;
import extended.tatu.wrapper.util.SensorWrapper;

/**
 *
 * @author Uellington Damasceno
 */
public class Main {

        public static AriesController ariesController;

        public static void main(String[] args) {
                try (InputStream input = Main.class.getResourceAsStream("broker.properties")) {
                        if (input == null) {
                                System.err.println("Sorry, unable to find config.properties.");
                                return;
                        }
                        Properties props = new Properties();
                        props.load(input);
                        String deviceId = CLI.getDeviceId(args)
                                        .orElse(UUID.randomUUID().toString());

                        String brokerIp = CLI.getBrokerIp(args)
                                .orElse(props.getProperty("brokerIp"));

                        String port = CLI.getPort(args)
                                .orElse(props.getProperty("port"));

                        String password = CLI.getPassword(args)
                                .orElse(props.getProperty("password"));

                        String user = CLI.getUsername(args)
                                .orElse(props.getProperty("username"));

                        String timeout = CLI.getTimeout(args)
                                .orElse("10000");

                        String agentIp = CLI.getAgentIp(args)
                                        .orElse("10000");

                        String agentPort = CLI.getAgentPort(args)
                                        .orElse("10000");

                        BrokerSettings brokerSettings = BrokerSettingsBuilder
                                        .builder()
                                        .setBrokerIp(brokerIp)
                                        .setPort(port)
                                        .setPassword(password)
                                        .setUsername(user)
                                        .deviceId(deviceId)
                                        .build();

                        if (CLI.hasParam("-ps", args)) {
                                MessageLogController.getInstance().createAndUpdateFileName(deviceId + "_messages_log.csv");
                                MessageLogController.getInstance().start();
                                MessageLogController.getInstance().setCanSaveData(true);
                        }
                        
                        if(CLI.hasParam("-ll", args)){
                                LatencyLogController.getInstance().createAndUpdateFileName(deviceId + "_latency_log.csv");
                                LatencyLogController.getInstance().start();
                                LatencyLogController.getInstance().setCanSaveData(true);
                        }

                        if(CLI.hasParam("-sl", args)){
                                ScoreLogController.getInstance().createAndUpdateFileName(deviceId + "_score_log.csv");
                                ScoreLogController.getInstance().start();
                                ScoreLogController.getInstance().setCanSaveData(true);
                        }

                        List<Sensor> sensors = readSensors("sensors.json", deviceId)
                                .stream()
                                .map(Sensor.class::cast)
                                .collect(toList());

                        ariesController = new AriesController(agentIp, agentPort);

                        JSONObject jsonInvitation = createInvitation(ariesController, deviceId);

                        FoTDevice device = new FoTDevice(deviceId, sensors, jsonInvitation);

                        BrokerUpdateCallback callback = new BrokerUpdateCallback(device);
                        callback.startUpdateBroker(brokerSettings, Long.parseLong(timeout), true, jsonInvitation);

                } catch (IOException ex) {
                        System.err.println("Sorry, unable to find sensors.json or not create pesistence file.");
                }
        }

        private static List<FoTSensor> readSensors(String fileName, String deviceName) throws IOException {
                try (var inputStream = Main.class.getResourceAsStream(fileName); var inputReader = new InputStreamReader(inputStream); var bufferedReader = new BufferedReader(inputReader)) {

                String textFile = bufferedReader.lines().collect(joining());
                JSONArray sensorsArray = new JSONArray(textFile);
                return SensorWrapper.getAllSensors(sensorsArray)
                        .stream()
                        .map(sensor -> new FoTSensor(deviceName, sensor))
                        .collect(toList());
                }
        
        }
        
        /**
         * Creating connection invitation.
         * 
         * @param ariesController - Aries controller with agent interaction methods.
         * @param label - Connection invite label.
         * @return JSONObject
         * @throws IOException
         */
        public static JSONObject createInvitation(AriesController ariesController, String label)
        throws IOException {
                System.out.println("Creating connection invitation...");

                CreateInvitationResponse createInvitationResponse = ariesController.createInvitation(label);

                String json = ariesController.getJsonInvitation(createInvitationResponse);

                System.out.println("Json Invitation: " + json);

                System.out.println("Invitation created!\n");

                JSONObject jsonInvitation = new JSONObject(json);

                return jsonInvitation;
        }
}
