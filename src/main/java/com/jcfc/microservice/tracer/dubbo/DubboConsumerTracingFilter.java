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
 * 基于brave实现的zipkin的filter，在dubbo协议客户端调用时使用
 *
 * @author zhangjinpeng
 * @version 0.0.1
 */
@Activate(group = Constants.CONSUMER)
public class DubboConsumerTracingFilter implements Filter {

    private final Tracer tracer;
    private final TraceContext.Extractor<Invocation> extractor;
    private final TraceContext.Injector<Invocation> injector;
    private final DubboTracingHandler handler;

    public DubboConsumerTracingFilter(){
        Tracing tracing = TracerManager.getTracing();
        extractor = tracing.propagation().extractor(DubboTracingHandler.GETTER);
        injector = tracing.propagation().injector(DubboTracingHandler.SETTER);
        tracer = tracing.tracer();
        handler = new DubboTracingHandler(tracer, Span.Kind.CLIENT);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (tracer == null) return invoker.invoke(invocation);

        Span dubboSpan = handler.handle(extractor, injector, invocation, invocation);

        Throwable error = null;
        Result result = null;
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(dubboSpan)) {
            result = invoker.invoke(invocation); // any downstream filters see Tracer.currentSpan
        } catch (RuntimeException | Error e) {
            error = e;
            throw e;
        } finally {
            //we have a synchronous response, so we can finish the span
            handler.handleSend(result, error, dubboSpan);
        }

        return result;
    }


}
