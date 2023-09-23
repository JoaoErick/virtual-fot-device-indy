package com.device.fot.virtual.controller;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import com.device.fot.virtual.model.BrokerSettings;
import com.device.fot.virtual.model.FoTDevice;

import extended.tatu.wrapper.enums.ExtendedTATUMethods;
import extended.tatu.wrapper.model.TATUMessage;
import extended.tatu.wrapper.util.ExtendedTATUWrapper;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author Uellington Damasceno
 */
public class BrokerUpdateCallback implements MqttCallback, Runnable {

    private FoTDevice device;
    private BrokerSettings brokerSettings;
    private Thread timeoutCounter;
    private String ip;

    public BrokerUpdateCallback(FoTDevice device) {
        this.device = device;
        this.ip = this.getIpAddress();
    }

    public void startUpdateBroker(BrokerSettings brokerSettings, double timeout, boolean retryConnect, JSONObject jsonInvitation) {
        if (this.device.isUpdating()) {
            System.out.println("Device já está atualizando.");
            return;
        }

        this.device.setIsUpdating(true);
        MqttConnectOptions newOptions = brokerSettings.getConnectionOptions();
        String connectionTopic = ExtendedTATUWrapper.getConnectionTopic();
        String message = ExtendedTATUWrapper.buildConnectMessage(device, ip, timeout, jsonInvitation);
        this.timeoutCounter = new Thread(this);
        this.timeoutCounter.setName("BROKER/UPDATE/TIMEOUT");

        try {
            MqttClient newClient = brokerSettings.getClient();

            newClient.setCallback(this);

            this.tryConnect(newClient, newOptions, retryConnect);

            newClient.subscribe(ExtendedTATUWrapper.getConnectionTopicResponse());
            newClient.publish(connectionTopic, new MqttMessage(message.getBytes()));

            this.brokerSettings = brokerSettings;

            this.timeoutCounter.start();

        } catch (MqttException ex) {
            brokerSettings.disconnectClient();
            device.setIsUpdating(false);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        String message = new String(mqttMessage.getPayload());
        TATUMessage tatuMessage = new TATUMessage(message);
        if (!tatuMessage.isResponse() || !tatuMessage.getMethod().equals(ExtendedTATUMethods.CONNACK)) {
            return;
        }
        this.timeoutCounter.interrupt();
        var json = new JSONObject(tatuMessage.getMessageContent());
        if (json.getJSONObject("BODY").getBoolean("CAN_CONNECT")) {
            this.device.updateBrokerSettings(brokerSettings);
            this.brokerSettings.getClient().unsubscribe(ExtendedTATUWrapper.getConnectionTopicResponse());
        } else {
            this.brokerSettings.disconnectClient();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10000L);
            if (this.device.isUpdating()) {
                this.device.setIsUpdating(false);
                this.brokerSettings.disconnectClient();
            }
        } catch (InterruptedException ex) {
        }
    }

    public void tryConnect(MqttClient client, MqttConnectOptions options, boolean retryConnect) {
        boolean connected = false;
        do {
            try {
                System.out.println("Conectando ao broker...");
                client.connect(options);
                connected = true;
            } catch (MqttException e) {
                System.out.println("Falha ao conectar ao broker.");
                if (!retryConnect) {
                    return;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                }
            }
        } while (!connected && retryConnect);
    }
    
    public final String getIpAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            return "UNKNOWN_HOST";
        }
    }
}
