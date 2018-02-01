package com.jcfc.microservice.tracer.dubbo;

import brave.Span;
import brave.Tracer;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.jcfc.microservice.tracer.TracerManager;
import com.jcfc.microservice.tracer.utils.NetworkUtils;
import com.jcfc.microservice.tracer.utils.SystemClock;
import zipkin2.Endpoint;

/**
 * 封装dubbo服务的操作
 *
 * @author zhangjinpeng
 * @version 1.0.0
 */

final class DubboTraingHandler {

    static final Propagation.Getter<Invocation, String> GETTER =
            new Propagation.Getter<Invocation, String>() {
                @Override public String get(Invocation carrier, String key) {
                    return carrier.getAttachment(key);
                }

                @Override public String toString() {
                    return "Invocation::getAttachment";
                }
            };

    static final Propagation.Setter<Invocation, String> SETTER =
            new Propagation.Setter<Invocation, String>() {
                @Override
                public void put(Invocation carrier, String key, String value) {
                    carrier.getAttachments().put(key, value);
                }

                @Override public String toString() {
                    return "Invocation::getAttachment::put";
                }
            };

    private final Tracer tracer;
    private final Span.Kind kind;

    DubboTraingHandler(Tracer tracer, Span.Kind kind){
        this.tracer = tracer;
        this.kind = kind;
    }


    <I> Span handleReceive(TraceContext.Extractor<I> extractor, TraceContext.Injector<I> injector, I carrier, Invocation invocation) {
        Span span = nextSpan(extractor.extract(carrier));
        if (span.isNoop()){
            return span;
        }

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(kind);
        span.name(getName(invocation));

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if(kind==Span.Kind.CONSUMER || kind== Span.Kind.CLIENT) {
                for (String key : invocation.getAttachments().keySet()) {
                    span.tag("Attachment-" + key, invocation.getAttachment(key));
                }
                span.tag("args", StringUtils.toArgumentString(invocation.getArguments()));
                span.tag("url", RpcContext.getContext().getUrl().toFullString());

                span.annotate(SystemClock.now(), TracerManager.ANNO_CS);
            }
            else {
                span.annotate(SystemClock.now(), TracerManager.ANNO_SR);
            }
        }
        //设置远程服务端地址
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder()
                .serviceName(RpcContext.getContext().getMethodName())
                .ip(RpcContext.getContext().getRemoteAddressString());
        span.remoteEndpoint(remoteEndpoint.build());

        if(kind==Span.Kind.CONSUMER || kind== Span.Kind.CLIENT) {
            // 将上下文信息注入到carrier
            injector.inject(span.context(), carrier);
        }

        return span.start();
    }

    private String getName(Invocation invocation){
//        StringBuffer name = new StringBuffer().append(invocation.getInvoker().getInterface().getName())
//                .append("::")
//                .append(invocation.getMethodName());
//        return name.toString();
        return invocation.getInvoker().getInterface().getName();
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
     *
     * <p>This is typically called once the response headers are sent, and after the span is {@link
     * brave.Tracer.SpanInScope#close() no longer in scope}.
     *
     */
    void handleSend(Result result, Throwable error, Span span) {
        if (span.isNoop()){
            return;
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if(result.getException()!=null) {
                span.tag("error", result.getException().getMessage());
            }
            if(error!=null) {
                span.tag("invoke-error", error.getMessage());
            }
            if(kind==Span.Kind.CONSUMER || kind== Span.Kind.CLIENT) {
                for (String key : result.getAttachments().keySet()) {
                    span.tag("result-" + key, result.getAttachment(key));
                }
                if(result.getValue()!=null) {
                    span.tag("result", result.getValue().toString());
                }
                span.annotate(SystemClock.now(), TracerManager.ANNO_CR);
            }
            else {
                span.annotate(SystemClock.now(), TracerManager.ANNO_SS);
            }
        } finally {
            span.finish();
        }
    }

}
