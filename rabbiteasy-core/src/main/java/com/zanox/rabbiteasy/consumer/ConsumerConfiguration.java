package com.zanox.rabbiteasy.consumer;

/**
 * A consumer configuration holds parameters to be set before enabling a consumer to
 * consume messages from the message broker.
 * 
 * @author christian.bick
 * @author uwe.janner
 * @author soner.dastan
 * 
 */
public class ConsumerConfiguration {

	private String queueName;
	private boolean autoAck = false;

	public ConsumerConfiguration(String queueName) {
		this.queueName = queueName;
	}

    public ConsumerConfiguration(String queueName, boolean autoAck) {
        this.queueName = queueName;
        this.autoAck = autoAck;
    }

	public String getQueueName() {
		return queueName;
	}

	public boolean isAutoAck() {
		return autoAck;
	}

}
