package com.redhat.lightblue.camel.pojo.outbound;

import static com.redhat.lightblue.util.test.AbstractJsonNodeTest.loadJsonNode;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
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
import com.redhat.lightblue.client.expression.query.ValueQuery;
import com.redhat.lightblue.client.projection.FieldProjection;
import com.redhat.lightblue.client.request.data.DataFindRequest;
import com.redhat.lightblue.client.response.DefaultLightblueResponse;

public class TestLightblueProducerRoute extends CamelTestSupport {

    @BeforeClass
    public static void prepareMetadataDatasources() {
        System.setProperty("mongo.datasource", "mongodata");
    }

    @ClassRule
    public static LightblueExternalResource lightblue = new LightblueExternalResource(new LightblueTestMethods() {

        @Override
        public JsonNode[] getMetadataJsonNodes() throws Exception {
            return new JsonNode[]{
                    loadJsonNode("./outbound/company.json")
            };
        }

    });

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start")
                // using recipient list here so I can construct the URI using camel simple expression language
                // I would also prefer to pass the lightblue query as json in the url. It should rather look like:
                // to(lightblue://getEntity?entityName=${entity}&entityVersion=${version}&query=${query}&projection=${projection}&sort=${sort}&range=${range})
                .recipientList(simple("lightblue://outboundTest${body}"))
                .to("mock:result");
            };
        };
    }

    @Before
    public void before() throws Exception {
        lightblue.loadData("company", "1.0.0", "./outbound/data/insert/companies.json");
        lightblue.init();
        LightblueEndpoint.registerLightblueClient("outboundTest", lightblue.getLightblueClient());
    }

    @Test
    public void testFetchEntityFromLightblue() throws Exception {

        // create data find request
        DataFindRequest findRequest = new DataFindRequest("company", "1.0.0");
        findRequest.where(ValueQuery.withValue("_id = " + 2));
        findRequest.select(FieldProjection.includeFieldRecursively("*"));
        String query = LightblueEndpoint.buildUriParameters(findRequest);

        template.sendBody(query);

        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(1);
        assertMockEndpointsSatisfied();

        DefaultLightblueResponse response = (DefaultLightblueResponse)mock.getExchanges().get(0).getIn().getBody();
        assertNotNull(response);
        assertFalse(response.hasError());
        assertEquals(1, response.parseMatchCount());
    }
}
