package com.redhat.lightblue.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;

import com.redhat.lightblue.client.response.LightblueResponse;

/**
 * The Lightblue consumer.
 */
public class LightblueEventConsumer extends ScheduledPollConsumer implements LightblueConsumerWithSetters {

    private final LightblueEndpoint endpoint;
    private String entityName;
    private String entityVersion;
    private String operation;
    private String body;

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    @Override
    public String getEntityVersion() {
        return entityVersion;
    }

    @Override
    public void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public String getOperation() {
        return operation;
    }

    @Override
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void setBody(String body) {
        this.body = body;
    }

    public LightblueEventConsumer(LightblueEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected int poll() throws Exception {
        Exchange exchange = endpoint.createExchange();

        // create a message body
        LightblueResponse response = LightblueProducer.callLightblue(
                endpoint.getLightblueClient(), getEntityName(), getEntityVersion(), getOperation(), getBody());
        exchange.getIn().setBody(response);
        try {
            // send message to next processor in the route
            getProcessor().process(exchange);
            return response.parseMatchCount(); // number of messages polled
        } finally {
            // log exception if an exception occurred and was not handled
            if (exchange.getException() != null) {
                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
            }
        }
    }

}
