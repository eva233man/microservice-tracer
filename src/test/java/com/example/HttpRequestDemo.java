package com.example;

import brave.Span;
import brave.Tracer;
import com.jcfc.microservice.tracer.http.HttpRequestTracingHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Created by zhangjinpeng on 2018/3/22.
 */

public class HttpRequestDemo {

    private final static HttpRequestTracingHandler handler = new HttpRequestTracingHandler();

    public static String sendGet(String url, String param) throws Exception {
        String result = "";
        BufferedReader in = null;
        String urlNameString = url;
        if(param!=null && !param.equals("")) {
            urlNameString = url + "?" + param;
        }
        URL realUrl = new URL(urlNameString);
        URLConnection connection = realUrl.openConnection();
        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setRequestProperty("user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        connection.setRequestProperty("Accept-Charset", "utf-8");
        connection.setRequestProperty("contentType", "utf-8");

        Span httpclientSpan = handler.handle(connection, param);
        Throwable error = null;
        try (Tracer.SpanInScope ws = handler.getTracer().withSpanInScope(httpclientSpan)) {
            connection.connect();
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result = result + line;
            }
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
            //we have a synchronous response, so we can finish the span
            handler.handleSend(result, error, httpclientSpan);
        }


        return result;
    }

    public static String sendGet(String url, Map<String, String> paramMap)
            throws Exception {
        return sendGet(url, formatParam(paramMap));
    }

    public static String sendPost(String url, String param) throws Exception {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        URL realUrl = new URL(url);
        URLConnection conn = realUrl.openConnection();
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        conn.setRequestProperty("Accept-Charset", "utf-8");
        conn.setRequestProperty("contentType", "utf-8");

        Span httpclientSpan = handler.handle(conn, param);
        Throwable error = null;
        try (Tracer.SpanInScope ws = handler.getTracer().withSpanInScope(httpclientSpan)) {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = new PrintWriter(conn.getOutputStream());
            out.print(param);
            out.flush();

            in = new BufferedReader(new InputStreamReader(conn.getInputStream(),
                    "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result = result + line;
            }
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            //we have a synchronous response, so we can finish the span
            handler.handleSend(result, error, httpclientSpan);
        }

        return result;
    }

    public static String sendPost(String url, Map<String, String> paramMap)
            throws Exception {
        return sendPost(url, formatParam(paramMap));
    }

    public static String formatParam(Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder();
        String param = "";
        if (null != paramMap && !paramMap.isEmpty()) {
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                sb.append("&").append(entry.getKey()).append("=")
                        .append(entry.getValue());
            }
            param = sb.substring(1);
        }
        return param;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("result:");
        System.out.println(HttpRequestDemo.sendGet("http://127.0.0.1:8080",""));
        Thread.sleep(1000000);
    }
}
