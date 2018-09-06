package com.jcfc.microservice.tracer.aop;

import brave.Span;
import brave.Tracer;
import brave.propagation.CurrentTraceContext;
import brave.propagation.SamplingFlags;
import brave.propagation.TraceContext;
import com.alibaba.fastjson.JSON;
import com.jcfc.microservice.tracer.TracerManager;
import com.jcfc.microservice.tracer.utils.NetworkUtils;
import com.jcfc.microservice.tracer.utils.StringUtils;
import com.jcfc.microservice.tracer.utils.SystemClock;
import org.aspectj.lang.ProceedingJoinPoint;
import zipkin2.Endpoint;

/**
 * 封装dubbo服务的操作
 *
 * @author zhangjinpeng
 * @version 1.0.0
 */

final class AopTracingHandler {

    private final Tracer tracer;
    private final CurrentTraceContext currentTraceContext;

    AopTracingHandler(Tracer tracer, CurrentTraceContext currentTraceContext) {
        this.tracer = tracer;
        this.currentTraceContext = currentTraceContext;
    }


    <I> Span handle(ProceedingJoinPoint joinPoint) {
        Span span = nextSpan();
        if (span.isNoop()) {
            return span;
        }

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(Span.Kind.CLIENT);
        span.name(getName(joinPoint));

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            maybeTag(span, "args", StringUtils.toArgumentString(joinPoint.getArgs()));
            maybeTag(span, "aop.url", joinPoint.toString());
            maybeTag(span, "component", "aop");
        }
        //设置远程服务端地址
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder()
                .ip(NetworkUtils.getLocalHost())
                .serviceName(joinPoint.getTarget().getClass().getSimpleName());
        span.remoteEndpoint(remoteEndpoint.build());

        return span.start();
    }


    private String getName(ProceedingJoinPoint joinPoint) {
        return joinPoint.toShortString();
    }

    /**
     * Creates a potentially noop span representing this request
     */
    private Span nextSpan() {
        TraceContext parent = currentTraceContext.get();
        if (parent != null){
            return tracer.newChild(parent); // inherit the sampling decision
        }
        return tracer.newTrace(SamplingFlags.SAMPLED);
    }

    /**
     * Finishes the server span after assigning it tags according to the response or error.
     * <p>
     * <p>This is typically called once the response headers are sent, and after the span is {@link
     * Tracer.SpanInScope#close() no longer in scope}.
     */
    void handleSend(Object object, Throwable error, Span span) {
        if (span.isNoop()) {
            return;
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if (error != null) {
                maybeTag(span, "error", "true");
                maybeTag(span, "invoke-error", error.getMessage());
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
