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
    private int instances = 1;

	public ConsumerConfiguration(String queueName) {
		this.queueName = queueName;
	}

    public ConsumerConfiguration(String queueName, boolean autoAck) {
        this.queueName = queueName;
        this.autoAck = autoAck;
    }

    public ConsumerConfiguration(String queueName, int instances) {
            this.queueName = queueName;
            this.instances = instances;
    }

    public ConsumerConfiguration(String queueName, boolean autoAck, int instances) {
        this.queueName = queueName;
        this.autoAck = autoAck;
        this.instances = instances;
    }

	public String getQueueName() {
		return queueName;
	}

	public boolean isAutoAck() {
		return autoAck;
	}

    public int getInstances() {
        return instances;
    }
}
