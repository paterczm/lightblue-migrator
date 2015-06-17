package com.redhat.lightblue.camel.pojo.outbound;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.lightblue.camel.LightblueEndpoint;
import com.redhat.lightblue.camel.LightblueExternalResource;
import com.redhat.lightblue.camel.LightblueExternalResource.LightblueTestMethods;
import com.redhat.lightblue.camel.dataformat.JacksonXmlDataFormat;
import com.redhat.lightblue.camel.model.Event;
import com.redhat.lightblue.camel.transformer.LightblueResponseTransformer;
import com.redhat.lightblue.camel.verifier.LightblueErrorVerifier;
import com.redhat.lightblue.client.expression.query.ValueQuery;
import com.redhat.lightblue.client.projection.FieldProjection;
import com.redhat.lightblue.client.request.data.DataFindRequest;

public class TestLightblueEventConsumerRoute extends CamelTestSupport {

    @BeforeClass
    public static void prepareMetadataDatasources() {
        System.setProperty("mongo.datasource", "mongodata");
    }

    @ClassRule
    public static LightblueExternalResource lightblue = new LightblueExternalResource(new LightblueTestMethods() {

        @Override
        public JsonNode[] getMetadataJsonNodes() throws Exception {
            return new JsonNode[]{
                    loadJsonNode("./outbound/event.json")
            };
        }

    });

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            @Override
            public void configure() {
                lightblue.init();
                LightblueEndpoint.registerLightblueClient("outboundPollingTest", lightblue.getLightblueClient());

                DataFindRequest findRequest = new DataFindRequest("event", "1.0.0");
                findRequest.where(ValueQuery.withValue("processed = false"));
                findRequest.select(FieldProjection.includeFieldRecursively("*"));

                from("lightblue://outboundPollingTest" + LightblueEndpoint.buildUriParameters(findRequest))
                        .bean(new LightblueErrorVerifier())
                        .bean(new LightblueResponseTransformer<Event[]>(Event[].class))
                        .marshal(new JacksonXmlDataFormat())
                        .to("mock:result");
            }
        };
    }

    @Before
    public void before() throws Exception {
        lightblue.loadData("event", "1.0.0", "./outbound/data/insert/events.json");
    }

    @Test
    public void testMessageFromLightblue() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(2);
        mock.expectedBodiesReceived(
                "<Events xmlns=\"\"><item><name>Something happened</name><processed>false</processed><_id>2</_id></item><item><name>Something else happened</name><processed>false</processed><_id>3</_id></item></Events>");
        assertMockEndpointsSatisfied();
    }
}
