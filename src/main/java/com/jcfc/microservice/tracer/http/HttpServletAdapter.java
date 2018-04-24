package com.jcfc.microservice.tracer.http;

import brave.http.HttpServerAdapter;
import zipkin2.Endpoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * servlet解析适配器
 *
 * @author zhangjinpeng
 * @version 1.0
 */

public class HttpServletAdapter
        extends HttpServerAdapter<HttpServletRequest, HttpServletResponse> {
    final ServletRuntime servlet = ServletRuntime.get();

    /**
     * Parses the remote address, via the "X-Forwarded-For" header, falling back to the
     * {@linkplain HttpServletRequest#getRemoteAddr() remote address}.
     */
    @Override
    public boolean parseClientAddress(HttpServletRequest req, Endpoint.Builder builder) {
        if (builder.parseIp(req.getHeader("X-Forwarded-For")) || builder.parseIp(req.getRemoteAddr())) {
            builder.port(req.getRemotePort());
            builder.serviceName(req.getHeader("Referer"));
            return true;
        }
        return false;
    }

    @Override
    public String method(HttpServletRequest request) {
        /**
         * 重写获取http方法的method方法
         * 除了get/post外，也加上URL地址
         */
        StringBuffer method = new StringBuffer().append(request.getMethod())
                .append("::")
                .append(request.getRequestURI());
        return method.toString();
    }

    @Override
    public String path(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @Override
    public String url(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            url.append('?').append(request.getQueryString());
        }
        return url.toString();
    }

    @Override
    public String requestHeader(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

    @Override
    public Integer statusCode(HttpServletResponse response) {
        return servlet.status(response);
    }
}
