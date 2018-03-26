package com.jcfc.microservice.tracer.http;

import brave.Tracing;
import brave.http.HttpTracing;
import com.jcfc.microservice.tracer.TracerManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URI;


/**
 * httpclient支持
 * Created by zhangjinpeng on 2018/3/21.
 */

public class TracingHttpClientBuilder {

    private HttpTracing httpTracing;

    public TracingHttpClientBuilder(){
        Tracing tracing = TracerManager.getTracing();
        httpTracing = HttpTracing.create(tracing);
    }

    public CloseableHttpClient newClient() {
        return brave.httpclient.TracingHttpClientBuilder.create(httpTracing).disableAutomaticRetries().build();
    }

    public void closeClient(CloseableHttpClient client) throws IOException {
        client.close();
    }

    public CloseableHttpResponse get(CloseableHttpClient client, String pathIncludingQuery)
            throws IOException {
        return client.execute(new HttpGet(URI.create(pathIncludingQuery)));
    }

    public CloseableHttpResponse post(CloseableHttpClient client, String pathIncludingQuery, String body)
            throws Exception {
        HttpPost post = new HttpPost(URI.create(pathIncludingQuery));
        post.setEntity(new StringEntity(body));
        return client.execute(post);
    }
}
