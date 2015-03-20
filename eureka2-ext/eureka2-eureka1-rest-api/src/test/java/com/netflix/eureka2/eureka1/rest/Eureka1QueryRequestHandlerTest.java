package com.netflix.eureka2.eureka1.rest;

import javax.ws.rs.core.MediaType;
import java.nio.charset.Charset;
import java.util.List;

import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka2.eureka1.rest.query.Eureka2RegistryViewCache;
import com.netflix.eureka2.registry.instance.InstanceInfo;
import com.netflix.eureka2.server.config.EurekaServerConfig;
import com.netflix.eureka2.server.config.EurekaServerConfig.EurekaServerConfigBuilder;
import com.netflix.eureka2.server.http.EurekaHttpServer;
import com.netflix.eureka2.testkit.data.builder.SampleInstanceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.netflix.eureka2.eureka1.rest.AbstractEureka1RequestHandler.ROOT_PATH;
import static com.netflix.eureka2.eureka1.rest.model.Eureka1ModelConverters.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tomasz Bak
 */
public class Eureka1QueryRequestHandlerTest {

    private static final com.netflix.appinfo.InstanceInfo V1_INSTANCE_1;
    private static final com.netflix.appinfo.InstanceInfo V1_INSTANCE_2;
    private static final Application V1_APPLICATION_1;
    private static final Applications V1_APPLICATIONS;

    static {
        List<InstanceInfo> app1 = SampleInstanceInfo.WebServer.clusterOf(2);
        V1_INSTANCE_1 = toEureka1xInstanceInfo(app1.get(0));
        V1_INSTANCE_2 = toEureka1xInstanceInfo(app1.get(1));
        V1_APPLICATION_1 = new Application("WebServer");
        V1_APPLICATION_1.addInstance(V1_INSTANCE_1);
        V1_APPLICATION_1.addInstance(V1_INSTANCE_2);

        V1_APPLICATIONS = new Applications();
        V1_APPLICATIONS.addApplication(V1_APPLICATION_1);
        V1_APPLICATIONS.setAppsHashCode("test");
    }

    private final EurekaServerConfig config = new EurekaServerConfigBuilder().withHttpPort(0).build();
    private final EurekaHttpServer httpServer = new EurekaHttpServer(config);
    private final Eureka2RegistryViewCache registryViewCache = mock(Eureka2RegistryViewCache.class);
    private Eureka1QueryRequestHandler queryResource;

    @Before
    public void setUp() throws Exception {
        queryResource = new Eureka1QueryRequestHandler(registryViewCache);
        httpServer.connectHttpEndpoint(ROOT_PATH, queryResource);
        httpServer.start();
    }

    @After
    public void tearDown() throws Exception {
        httpServer.stop();
    }

    @Test
    public void testGetAllApplicationsInJson() throws Exception {
        doTestGetAllApplications(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetAllApplicationsInXml() throws Exception {
        doTestGetAllApplications(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetApplicationsDeltaInJson() throws Exception {
        doTestGetApplicationsDelta(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetApplicationsDeltaInXml() throws Exception {
        doTestGetApplicationsDelta(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetApplicationsWithVipInJson() throws Exception {
        doTestGetApplicationsWithVip(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetApplicationsWithVipInXml() throws Exception {
        doTestGetApplicationsWithVip(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetApplicationsWithSecureVipInJson() throws Exception {
        doTestGetApplicationsWithSecureVip(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetApplicationsWithSecureVipInXml() throws Exception {
        doTestGetApplicationsWithSecureVip(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetApplicationInJson() throws Exception {
        doTestGetApplication(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetApplicationInXml() throws Exception {
        doTestGetApplication(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetByApplicationAndInstanceIdInJson() throws Exception {
        doTestGetByApplicationAndInstanceId(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetByApplicationAndInstanceIdInXml() throws Exception {
        doTestGetByApplicationAndInstanceId(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void testGetByInstanceIdInJson() throws Exception {
        doTestGetByInstanceId(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void testGetByInstanceIdInXml() throws Exception {
        doTestGetByInstanceId(MediaType.APPLICATION_XML_TYPE);
    }

    private void doTestGetAllApplications(MediaType mediaType) {
        when(registryViewCache.findAllApplications()).thenReturn(V1_APPLICATIONS);

        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, ROOT_PATH + "/apps");
        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("applications"), is(true));
    }

    private void doTestGetApplicationsDelta(MediaType mediaType) {
        when(registryViewCache.findAllApplicationsDelta()).thenReturn(V1_APPLICATIONS);

        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, ROOT_PATH + "/apps/delta");
        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("applications"), is(true));
    }

    private void doTestGetApplicationsWithSecureVip(MediaType mediaType) {
        when(registryViewCache.findApplicationsBySecureVip(V1_INSTANCE_1.getSecureVipAddress())).thenReturn(V1_APPLICATIONS);

        String path = ROOT_PATH + "/svips/" + V1_INSTANCE_1.getSecureVipAddress();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, path);

        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("application"), is(true));
    }

    private void doTestGetApplicationsWithVip(MediaType mediaType) {
        when(registryViewCache.findApplicationsByVip(V1_INSTANCE_1.getVIPAddress())).thenReturn(V1_APPLICATIONS);

        String path = ROOT_PATH + "/vips/" + V1_INSTANCE_1.getVIPAddress();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, path);

        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("application"), is(true));
    }

    private void doTestGetApplication(MediaType mediaType) {
        when(registryViewCache.findApplication(V1_APPLICATION_1.getName())).thenReturn(V1_APPLICATION_1);

        String path = ROOT_PATH + "/apps/" + V1_APPLICATION_1.getName();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, path);

        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("application"), is(true));
    }

    private void doTestGetByApplicationAndInstanceId(MediaType mediaType) {
        when(registryViewCache.findInstance(V1_INSTANCE_1.getId())).thenReturn(V1_INSTANCE_1);

        String path = ROOT_PATH + "/apps/" + V1_INSTANCE_1.getAppName() + '/' + V1_INSTANCE_1.getId();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, path);

        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("instance"), is(true));
    }

    private void doTestGetByInstanceId(MediaType mediaType) {
        when(registryViewCache.findInstance(V1_INSTANCE_1.getId())).thenReturn(V1_INSTANCE_1);

        String path = ROOT_PATH + "/instances/" + V1_INSTANCE_1.getId();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.create(HttpMethod.GET, path);

        String response = handleGetRequest(request, mediaType);
        assertThat(response.contains("instance"), is(true));
    }

    private String handleGetRequest(HttpClientRequest<ByteBuf> request, final MediaType mediaType) {
        request.getHeaders().add(Names.ACCEPT, mediaType);
        return RxNetty.createHttpClient("localhost", httpServer.serverPort()).submit(request)
                .flatMap(new Func1<HttpClientResponse<ByteBuf>, Observable<String>>() {
                    @Override
                    public Observable<String> call(HttpClientResponse<ByteBuf> response) {
                        if (!response.getStatus().equals(HttpResponseStatus.OK)) {
                            return Observable.error(new Exception("invalid status code " + response.getStatus()));
                        }
                        String bodyContentType = response.getHeaders().get(Names.CONTENT_TYPE);
                        if (!mediaType.toString().equals(bodyContentType)) {
                            return Observable.error(new Exception("invalid Content-Type header in response " + bodyContentType));
                        }
                        return loadResponseBody(response);
                    }
                }).toBlocking().first();
    }

    private static Observable<String> loadResponseBody(HttpClientResponse<ByteBuf> response) {
        return response.getContent()
                .reduce(new StringBuilder(), new Func2<StringBuilder, ByteBuf, StringBuilder>() {
                    @Override
                    public StringBuilder call(StringBuilder accumulator, ByteBuf byteBuf) {
                        return accumulator.append(byteBuf.toString(Charset.defaultCharset()));
                    }
                }).map(new Func1<StringBuilder, String>() {
                    @Override
                    public String call(StringBuilder builder) {
                        return builder.toString();
                    }
                });
    }
}