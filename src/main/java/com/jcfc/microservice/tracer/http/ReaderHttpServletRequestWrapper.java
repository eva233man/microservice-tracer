package com.jcfc.microservice.tracer.http;

import jodd.io.StreamUtil;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * Created by zhangjinpeng on 2018/4/27.
 */
public class ReaderHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body; //用于保存读取body中数据

    public ReaderHttpServletRequestWrapper(HttpServletRequest request)
            throws IOException {
        super(request);
        body = StreamUtil.readBytes(request.getReader(), "UTF-8");
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }
}
