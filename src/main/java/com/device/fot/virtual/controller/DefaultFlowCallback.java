package com.device.fot.virtual.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import com.device.fot.virtual.model.BrokerSettings;
import com.device.fot.virtual.model.BrokerSettingsBuilder;
import com.device.fot.virtual.model.FoTDevice;
import com.device.fot.virtual.model.FoTSensor;
import com.device.fot.virtual.model.NullFoTSensor;

import extended.tatu.wrapper.model.TATUMessage;
import extended.tatu.wrapper.util.TATUWrapper;

/**
 *
 * @author Uelligton Damasceno
 */
public class DefaultFlowCallback implements MqttCallback {

    private FoTDevice device;
    private BrokerUpdateCallback brokerUpdateController;

    public DefaultFlowCallback(FoTDevice device) {
        this.device = device;
        this.brokerUpdateController = new BrokerUpdateCallback(device);
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        TATUMessage tatuMessage = new TATUMessage(mqttMessage.getPayload());
        MqttMessage mqttResponse = new MqttMessage();
        FoTSensor sensor;

        System.out.println("============================");
        System.out.println("MQTT_MESSAGE: " + new String(mqttMessage.getPayload()));
        System.out.println("TOPIC: " + topic);
        System.out.println("MY_MESSAGE: " + tatuMessage);

        switch (tatuMessage.getMethod()) {
            case FLOW:
                sensor = (FoTSensor) device.getSensorBySensorId(tatuMessage.getTarget())
                        .orElse(NullFoTSensor.getInstance());
                JSONObject flow = new JSONObject(tatuMessage.getMessageContent());
                sensor.startFlow(flow.getInt("collect"), flow.getInt("publish"));
                break;
            case GET:
                sensor = (FoTSensor) device.getSensorBySensorId(tatuMessage.getTarget())
                        .orElse(NullFoTSensor.getInstance());
                String jsonResponse = TATUWrapper.buildGetMessageResponse(device.getId(),
                        sensor.getId(),
                        sensor.getCurrentValue());

                mqttResponse.setPayload(jsonResponse.getBytes());
                String publishTopic = TATUWrapper.buildTATUResponseTopic(device.getId());
                this.device.publish(publishTopic, mqttResponse);
                break;
            case SET:
                if (tatuMessage.getTarget().equalsIgnoreCase("brokerMqtt") && !this.device.isUpdating()) {
                    var newMessage = tatuMessage.getMessageContent();

                    var newBrokerSettingsJson = new JSONObject(newMessage);

                    BrokerSettings newBrokerSettings = BrokerSettingsBuilder.builder()
                            .deviceId(newBrokerSettingsJson.getString("id"))
                            .setBrokerIp(newBrokerSettingsJson.getString("url"))
                            .setPort(newBrokerSettingsJson.getString("port"))
                            .setUsername(newBrokerSettingsJson.getString("user"))
                            .setPassword(newBrokerSettingsJson.getString("password"))
                            .build();

                    this.brokerUpdateController.startUpdateBroker(newBrokerSettings, 10.000, false, device.getJsonInvitation());
                } else {
                    System.out.println("The device is updating: " + this.device.isUpdating());
                }
                break;
            case EVT:
                break;
            case POST:
                break;
            case INVALID:
                System.out.println("Invalid message!");
                break;
            default:
                throw new AssertionError(tatuMessage.getMethod().name());
        }
        System.out.println("============================");

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {

    }

    @Override
    public void connectionLost(Throwable cause) {
        Logger.getLogger(DefaultFlowCallback.class.getName()).log(Level.SEVERE, null, cause);
    }
}
