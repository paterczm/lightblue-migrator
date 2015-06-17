package com.redhat.lightblue.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.lang.StringUtils;

import com.redhat.lightblue.client.LightblueClient;
import com.redhat.lightblue.client.request.AbstractLightblueDataRequest;
import com.redhat.lightblue.client.request.data.DataDeleteRequest;
import com.redhat.lightblue.client.request.data.DataFindRequest;
import com.redhat.lightblue.client.request.data.DataInsertRequest;
import com.redhat.lightblue.client.request.data.DataSaveRequest;
import com.redhat.lightblue.client.request.data.DataUpdateRequest;
import com.redhat.lightblue.client.request.data.LiteralDataRequest;
import com.redhat.lightblue.client.response.LightblueResponse;

/**
 * The Lightblue producer.
 */
public class LightblueProducer extends DefaultProducer implements LightblueConsumerWithSetters {

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

    public LightblueProducer(LightblueEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getOut().setBody(callLightblue(endpoint.getLightblueClient(), getEntityName(), getEntityVersion(), getOperation(), getBody()));
    }

    public static LightblueResponse callLightblue(LightblueClient client, String entityName, String entityVersion, String operation, String json) {

        AbstractLightblueDataRequest dataRequest;
        //TODO Not ideal, find better way.
        switch (operation) {
            case "insert":
                dataRequest = new DataInsertRequest();
                break;
            case "update":
                dataRequest = new DataUpdateRequest();
                break;
            case "save":
                dataRequest = new DataSaveRequest();
                break;
            case "find":
                dataRequest = new DataFindRequest();
                break;
            case "delete":
                dataRequest = new DataDeleteRequest();
                break;
            default:
                throw new RuntimeException("Unsupported operation: " + operation);
        }

        LiteralDataRequest request = new LiteralDataRequest(
                entityName,
                ((StringUtils.isEmpty(entityVersion)) ? null : entityVersion),
                json,
                dataRequest.getHttpMethod(),
                dataRequest.getOperationPathParam());

        return client.data(request);
    }

}
