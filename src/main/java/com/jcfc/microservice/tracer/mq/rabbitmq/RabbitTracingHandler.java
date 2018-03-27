package com.jcfc.microservice.tracer.mq.rabbitmq;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.jcfc.microservice.tracer.TracerManager;
import zipkin2.Endpoint;

import java.util.Map;

/**
 * 封装rabbitmq消息的操作
 *
 * @author zhangjinpeng
 * @version 1.0.0
 */

public class RabbitTracingHandler {

    static final Propagation.Getter<RabbitmqMessage, String> GETTER =
            new Propagation.Getter<RabbitmqMessage, String>() {
                @Override
                public String get(RabbitmqMessage carrier, String key) {
                    Object value = carrier.getHeaders().get(key);
                    return value == null ? null : value.toString();
                }

                @Override
                public String toString() {
                    return "RabbitmqMessage.getHeaders()::get";
                }
            };

    static final Propagation.Setter<RabbitmqMessage, String> SETTER =
            new Propagation.Setter<RabbitmqMessage, String>() {
                @Override
                public void put(RabbitmqMessage carrier, String key, String value) {
                    carrier.getHeaders().put(key, value);
                }

                @Override
                public String toString() {
                    return "RabbitmqMessage.getHeaders()::put";
                }
            };
    private final TraceContext.Injector<RabbitmqMessage> injector;
    private final TraceContext.Extractor<RabbitmqMessage> extractor;
    private final Tracer tracer;
    private final Span.Kind kind;
    private final Tracing tracing = TracerManager.getTracing();


    public RabbitTracingHandler(Span.Kind kind) {
        tracer = tracing.tracer();
        this.kind = kind;
        this.injector = tracing.propagation().injector(SETTER);
        this.extractor = tracing.propagation().extractor(GETTER);
    }

    public Tracer getTracer() {
        return tracer;
    }

    private TraceContextOrSamplingFlags extractTraceContextAndRemoveHeaders(RabbitmqMessage message) {
        TraceContextOrSamplingFlags extracted = extractor.extract(message);
        if (kind == Span.Kind.CONSUMER) {
            for (String key : tracing.propagation().keys()) {
                message.getHeaders().remove(key);
            }
        }
        return extracted;
    }

    public Span handle(RabbitmqMessage message) {
        final Span span = nextSpan(extractTraceContextAndRemoveHeaders(message));
        if (kind == Span.Kind.PRODUCER) {
            // 将上下文信息注入到carrier
            injector.inject(span.context(), message);
        }

        if (span.isNoop()) {
            return span;
        }

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(kind);
        if (kind == Span.Kind.CONSUMER) {
            span.name("consume");
        } else {
            span.name("publish");
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if (kind == Span.Kind.PRODUCER) {
                span.tag("produce-msg", message.getMessage());
            }
            span.tag("component", "rabbitmq");
            span.tag("rabbit.channel", message.getBrokeUrl());
        }
        //设置远程服务端地址
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder()
                .serviceName(message.getQueueName());
        span.remoteEndpoint(remoteEndpoint.build());

        return span.start();
    }

    /**
     * Creates a potentially noop span representing this request
     */
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
    public void handleSend(RabbitmqMessage message, Throwable error, Span span) {
        if (span.isNoop()) {
            return;
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if (kind == Span.Kind.CONSUMER) {
                span.tag("consume-msg", message.getMessage());
            }
            if (error != null) {
                span.tag("error", "true");
                span.tag("mq-error", error.getMessage());
            }
        } finally {
            span.finish();
        }
    }

}
