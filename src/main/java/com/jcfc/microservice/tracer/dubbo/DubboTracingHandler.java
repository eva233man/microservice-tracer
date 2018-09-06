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
import zipkin2.Endpoint;

/**
 * 封装dubbo服务的操作
 *
 * @author zhangjinpeng
 * @version 1.0.0
 */

final class DubboTracingHandler {

    static final Propagation.Getter<Invocation, String> GETTER =
            new Propagation.Getter<Invocation, String>() {
                @Override
                public String get(Invocation carrier, String key) {
                    return carrier.getAttachment(key);
                }

                @Override
                public String toString() {
                    return "Invocation::getAttachment";
                }
            };

    static final Propagation.Setter<Invocation, String> SETTER =
            new Propagation.Setter<Invocation, String>() {
                @Override
                public void put(Invocation carrier, String key, String value) {
                    carrier.getAttachments().put(key, value);
                }

                @Override
                public String toString() {
                    return "Invocation::getAttachment::put";
                }
            };

    private final Tracer tracer;
    private final Span.Kind kind;

    DubboTracingHandler(Tracer tracer, Span.Kind kind) {
        this.tracer = tracer;
        this.kind = kind;
    }


    <I> Span handle(TraceContext.Extractor<I> extractor, TraceContext.Injector<I> injector, I carrier, Invocation invocation) {
        final Span span = nextSpan(extractor.extract(carrier));
        if (kind == Span.Kind.CLIENT) {
            // 将上下文信息注入到carrier
            injector.inject(span.context(), carrier);
        }


        if (span.isNoop()) {
            return span;
        }

        // all of the parsing here occur before a timestamp is recorded on the span
        span.kind(kind);
        span.name(getName(invocation));

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            String url = invocation.getInvoker().getUrl().toFullString();
//            if (kind == Span.Kind.CLIENT) {
                maybeTag(span, "args", StringUtils.toArgumentString(invocation.getArguments()));
                maybeTag(span, "dubbo.url", url);
                maybeTag(span, "component", "dubbo");
//            }
        }

        //设置远程通讯端地址
        Endpoint.Builder remoteEndpoint = Endpoint.newBuilder()
                .serviceName(RpcContext.getContext().getUrl().getParameter("application","defualt-app"))
                .ip(RpcContext.getContext().getRemoteHost())
                .port(RpcContext.getContext().getRemotePort());
//                .ip(invocation.getInvoker().getUrl().getIp())
//                .port(invocation.getInvoker().getUrl().getPort());
        span.remoteEndpoint(remoteEndpoint.build());

        return span.start();
    }

    private String getName(Invocation invocation) {
//        StringBuffer name = new StringBuffer().append(invocation.getInvoker().getInterface().getName())
//                .append("::")
//                .append(invocation.getMethodName());
//        return name.toString();
        String name = invocation.getInvoker().getInterface().getSimpleName();
        if("DubboInterface".equals(name)){
            try {
                return invocation.getArguments()[0].toString();
            }
            catch (Exception e){
                return name;
            }
        }

        return name;
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

    private static void maybeTag(Span span, String tag, String value) {
        if (value != null && value.length()<100000) {
            span.tag(tag, value);
        }
    }

    /**
     * Finishes the server span after assigning it tags according to the response or error.
     * <p>
     * <p>This is typically called once the response headers are sent, and after the span is {@link
     * brave.Tracer.SpanInScope#close() no longer in scope}.
     */
    void handleSend(Result result, Throwable error, Span span) {
        if (span.isNoop()) {
            return;
        }

        // Ensure user-code can read the current trace context
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            if (result.getException() != null) {
                maybeTag(span, "error", "true");
                maybeTag(span, "error-msg", result.getException().getMessage());
            }
            if (error != null) {
                maybeTag(span, "error", "true");
                maybeTag(span, "invoke-error", error.getMessage());
            }
//            if (kind == Span.Kind.CLIENT) {
//                for (String key : result.getAttachments().keySet()) {
//                    span.tag("result-" + key, result.getAttachment(key));
//                }

                if (result.getValue() != null) {
                    maybeTag(span, "result", result.getValue().toString());
                }
//            }
        } finally {
            span.finish();
        }
    }

}
