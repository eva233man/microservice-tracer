package com.jcfc.microservice.tracer.dubbo;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import com.jcfc.microservice.tracer.TracerManager;

/**
 * 基于brave实现的zipkin的filter，在dubbo协议服务端响应时使用
 *
 * @author zhangjinpeng
 * @version 0.0.1
 */
@Activate(group = Constants.PROVIDER)
public class DubboProviderTracingFilter implements Filter {

    private final Tracer tracer;
    private final TraceContext.Extractor<Invocation> extractor;
    private final TraceContext.Injector<Invocation> injector;
    private final DubboTraingHandler handler;

    public DubboProviderTracingFilter(){
        Tracing tracing = TracerManager.getTracing();
        extractor = tracing.propagation().extractor(DubboTraingHandler.GETTER);
        injector = tracing.propagation().injector(DubboTraingHandler.SETTER);
        tracer = tracing.tracer();
        handler = new DubboTraingHandler(tracer, Span.Kind.SERVER);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        Span dubboSpan = handler.handleReceive(extractor, injector, invocation, invocation);

        Throwable error = null;
        Result result = null;
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(dubboSpan)) {
            result = invoker.invoke(invocation); // any downstream filters see Tracer.currentSpan
        } catch (RuntimeException | Error e) {
            error = e;
            throw e;
        } finally {
            if(error!=null) {
                invocation.getAttachments().put("invoke-error", error.getMessage());
            }
            //we have a synchronous response, so we can finish the span
            handler.handleSend(result, error, dubboSpan);
        }

        return result;
    }
}
