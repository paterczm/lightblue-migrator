package com.redhat.lightblue.camel;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointConfiguration;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.api.management.ManagedAttribute;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.commons.lang.StringUtils;

import com.redhat.lightblue.client.LightblueClient;
import com.redhat.lightblue.client.request.AbstractLightblueDataRequest;
import com.redhat.lightblue.client.request.data.DataDeleteRequest;
import com.redhat.lightblue.client.request.data.DataFindRequest;
import com.redhat.lightblue.client.request.data.DataInsertRequest;
import com.redhat.lightblue.client.request.data.DataSaveRequest;
import com.redhat.lightblue.client.request.data.DataUpdateRequest;

/**
 * Represents a Lightblue endpoint.
 */
@UriEndpoint(scheme = "lightblue", title = "Lightblue", syntax = "lightblue:name", consumerClass = LightblueEventConsumer.class, label = "Lightblue")
public class LightblueEndpoint extends DefaultEndpoint {

    /**
     * Internal registry of {@link LightblueClient}s, keyed by hostname.
     */
    private final static Map<String, LightblueClient> registeredClients = new HashMap<String, LightblueClient>();

    @UriParam
    @Metadata(required = "true")
    private String operation;
    @UriParam
    @Metadata(required = "true")
    private String entityName;
    @UriParam
    @Metadata(required = "true")
    private String entityVersion;
    @UriParam
    @Metadata(required = "true")
    private String request;

    public static void registerLightblueClient(String hostname, LightblueClient lightblueClient) {
        registeredClients.put(hostname, lightblueClient);
    }

    public static LightblueClient getLightblueClient(String hostname) {
        return registeredClients.get(hostname);
    }

    public static String buildUriParameters(AbstractLightblueDataRequest request) {
        StringBuffer builder = new StringBuffer("?");

        //TODO Not ideal, find better way.
        String operation;
        if (request instanceof DataInsertRequest) {
            operation = "insert";
        }
        else if (request instanceof DataFindRequest) {
            operation = "find";
        }
        else if (request instanceof DataUpdateRequest) {
            operation = "update";
        }
        else if (request instanceof DataSaveRequest) {
            operation = "save";
        }
        else if (request instanceof DataDeleteRequest) {
            operation = "delete";
        }
        else {
            throw new UnsupportedOperationException("Request type not supported: " + request.getClass());
        }

        builder.append("operation=").append(operation).append("&");
        builder.append("entityName=").append(request.getEntityName()).append("&");
        if (!StringUtils.isEmpty(request.getEntityVersion())) {
            builder.append("entityVersion=").append(request.getEntityVersion()).append("&");
        }
        // @TODO: refactor this
        try {
            builder.append("request=").append(URLEncoder.encode(request.getBody(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public LightblueEndpoint() {}

    public LightblueEndpoint(String uri, LightblueComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        LightblueProducer producer = new LightblueProducer(this);
        producer.setEntityName(getEntityName());
        producer.setEntityVersion(getEntityVersion());
        producer.setOperation(getOperation());
        producer.setBody(getRequest());
        return producer;
    }

    @Override
    public LightblueConsumer createConsumer(Processor processor) throws Exception {
        LightblueConsumerWithSetters consumer = new LightblueEventConsumer(this, processor);
        consumer.setEntityName(getEntityName());
        consumer.setEntityVersion(getEntityVersion());
        consumer.setOperation(getOperation());
        consumer.setBody(getRequest());

        return consumer;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public LightblueClient getLightblueClient() {
        String hostname = getEndpointConfiguration().getParameter(EndpointConfiguration.URI_HOST).toString();
        return getLightblueClient(hostname);
    }

    @ManagedAttribute(description = "Operation to be performed: [insert, update, save, find, delete]")
    public String getOperation() {
        return operation;
    }

    @ManagedAttribute(description = "Operation to be performed: [insert, update, save, find, delete]")
    public void setOperation(String operation) {
        this.operation = operation;
    }

    @ManagedAttribute(description = "Name of lightblue entity")
    public String getEntityName() {
        return entityName;
    }

    @ManagedAttribute(description = "Name of lightblue entity")
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    @ManagedAttribute(description = "Version of lightblue entity")
    public String getEntityVersion() {
        return entityVersion;
    }

    @ManagedAttribute(description = "Version of lightblue entity")
    public void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }

    @ManagedAttribute(description = "Json body of request")
    public String getRequest() {
        return request;
    }

    @ManagedAttribute(description = "Json body of request")
    public void setRequest(String request) {
        this.request = request;
    }
}
