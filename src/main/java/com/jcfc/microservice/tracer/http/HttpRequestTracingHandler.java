package com.jcfc.microservice.tracer.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.*;
import com.alibaba.fastjson.JSON;
import com.jcfc.microservice.tracer.TracerManager;
import com.jcfc.microservice.tracer.utils.NetworkUtils;
import zipkin2.Endpoint;

import java.net.URLConnection;

/**
 * httpRequest方式
 * Created by zhangjinpeng on 2018/3/22.
 */

public class HttpRequestTracingHandler {
    private final Tracer tracer;

    static final Propagation.Setter<URLConnection, String> SETTER =
            new Propagation.Setter<URLConnection, String>() {
                @Override
                public void put(URLConnection carrier, String key, String value) {
                    carrier.setRequestProperty(key, value);
                }

                @Override public String toString() {
                    return "URLConnection::setRequestProperty";
                }
            };
    static final Propagation.Getter<URLConnection, String> GETTER =
            new Propagation.Getter<URLConnection, String>() {
                @Override public String get(URLConnection carrier, String key) {
                    return carrier.getRequestProperty(key);
                }

                @Override public String toString() {
                    return "URLConnection::getRequestProperty";
                }
            };

    private final TraceContext.Injector<URLConnection> injector;
    private final TraceContext.Extractor<URLConnection> extractor;

    public HttpRequestTracingHandler(){
        Tracing tracing = TracerManager.getInstance().getTracing();
        tracer = tracing.tracer();
        injector = tracing.propagation().injector(SETTER);
        extractor = tracing.propagation().extractor(GETTER);
    }

    public Tracer getTracer(){
        return tracer;
    }

    public Span handle(URLConnection connection, String args) {
        final Span span = nextSpan(extractor.extract(connection));
        injector.inject(span.context(), connection);

        if (span.isNoop()) {
            return span;
        }

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(Span.Kind.CLIENT);
        span.name(getName(connection));

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            maybeTag(span,"args", args);
            maybeTag(span,"http.url", connection.getURL().toString());
//            span.tag("component", "http");
            maybeTag(span,"component-client", "httprequest");
        }
        //设置远程服务端地址
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder()
                .ip(NetworkUtils.getLocalHost());
        span.remoteEndpoint(remoteEndpoint.build());

        return span.start();
    }


    private String getName(URLConnection connection) {
        return connection.getURL().getPath();
    }

    /** Creates a potentially noop span representing this request */
    private Span nextSpan(TraceContextOrSamplingFlags extracted) {
        if (extracted.sampled() == null) { // Otherwise, try to make a new decision
            extracted = extracted.sampled(true);
        }
        return extracted.context() != null
                ? tracer.joinSpan(extracted.context())
                : tracer.nextSpan(extracted);
    }
    /**
     * Finishes the server span after assigning it tags according to the response or error.
     * <p>
     * <p>This is typically called once the response headers are sent, and after the span is {@link
     * Tracer.SpanInScope#close() no longer in scope}.
     */
    public void handleSend(Object object, Throwable error, Span span) {
        if (span.isNoop()) {
            return;
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if (error != null) {
                maybeTag(span,"error", "true");
                maybeTag(span,"httprequest-error", error.getMessage());
            }
            maybeTag(span,"result", JSON.toJSONString(object));
        } finally {
            span.finish();
        }
    }

    private static void maybeTag(Span span, String tag, String value) {
        if (value != null && value.length()<100000) {
            span.tag(tag, value);
        }
    }

}
