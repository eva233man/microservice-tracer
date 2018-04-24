package com.jcfc.microservice.tracer.http;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation;
import brave.propagation.TraceContext;
import com.alibaba.fastjson.JSON;
import com.jcfc.microservice.tracer.TracerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 基于brave实现的zipkin的filter，在http协议调用时使用
 *
 * @author zhangjinpeng
 * @version 0.0.1
 */
public class HttpTracingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(HttpTracingFilter.class);

    static final Propagation.Getter<HttpServletRequest, String> GETTER =
            new Propagation.Getter<HttpServletRequest, String>() {
                @Override public String get(HttpServletRequest carrier, String key) {
                    return carrier.getHeader(key);
                }

                @Override public String toString() {
                    return "HttpServletRequest::getHeader";
                }
            };

    private Tracing tracing;

    private final ServletRuntime servlet = ServletRuntime.get();
    private final Tracer tracer;
    private final HttpServerHandler handler;
    private final TraceContext.Extractor<HttpServletRequest> extractor;

    public HttpTracingFilter() {
        tracing = TracerManager.getTracing();
        HttpTracing httpTracing = HttpTracing.create(tracing);

        tracer = httpTracing.tracing().tracer();
        handler = HttpServerHandler.create(httpTracing, new HttpServletAdapter());
        extractor = httpTracing.tracing().propagation().extractor(GETTER);
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
//        tracing = TracerManager.getTracing();

//        tracingFilter = TracingFilter.create(tracing);
//        tracingFilter.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = servlet.httpResponse(response);

        // Prevent duplicate spans for the same request
        if (request.getAttribute("TracingFilter") != null) {
            chain.doFilter(request, response);
            return;
        }

        request.setAttribute("TracingFilter", "true");

        Span span = handler.handleReceive(extractor, httpRequest);
        Throwable error = null;
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            span.tag("http.url" , request.getLocalAddr());
            span.tag("http.port" , Integer.toString(request.getLocalPort()));
            span.tag("peer.address" , request.getRemoteAddr());
            span.tag("peer.port" , Integer.toString(request.getRemotePort()));
            span.tag("component", "http");
            span.tag("args", JSON.toJSONString(request.getParameterMap()));

            chain.doFilter(httpRequest, httpResponse); // any downstream filters see Tracer.currentSpan

            span.tag("http.status", Integer.toString(httpResponse.getStatus()));
        } catch (IOException | ServletException | RuntimeException | Error e) {
            error = e;
            span.tag("error", "true");
            throw e;
        } finally {
            if (servlet.isAsync(httpRequest)) { // we don't have the actual response, handle later
                servlet.handleAsync(handler, httpRequest, span);
            } else { // we have a synchronous response, so we can finish the span
                handler.handleSend(httpResponse, error, span);
            }
        }
    }

    @Override
    public void destroy() {
        tracing.close();
    }

}
