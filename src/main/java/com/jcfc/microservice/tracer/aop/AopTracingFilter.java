package com.jcfc.microservice.tracer.aop;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.jcfc.microservice.tracer.TracerManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 基于Spring AOP方式实现的tracing的filter
 *
 * @author zhangjinpeng
 * @version 1.0.0
 */

@Component
@Aspect
public class AopTracingFilter {
    private final Tracer tracer;
    private final AopTracingHandler handler;

    public AopTracingFilter(){
        Tracing tracing = TracerManager.getTracing();
        tracer = tracing.tracer();
        handler = new AopTracingHandler(tracer, tracing.currentTraceContext());
    }

    /**
     * 环绕切面
     *
     * @param joinPoint 切点
     * @return
     * @throws Throwable
     */
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Span aopSpan = handler.handleReceive(joinPoint);

        Throwable error = null;
        Object object = null;
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(aopSpan)) {
            object = joinPoint.proceed();  //业务方法的执行
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            //we have a synchronous response, so we can finish the span
            handler.handleSend(object, error, aopSpan);
        }

        return object;
    }
}
