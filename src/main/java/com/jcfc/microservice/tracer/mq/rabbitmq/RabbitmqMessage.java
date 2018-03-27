package com.jcfc.microservice.tracer.mq.rabbitmq;

import java.util.HashMap;
import java.util.Map;

/**
 * 封装rabbitmq的消息内容、报文头
 * Created by zhangjinpeng on 2018/3/27.
 */

public class RabbitmqMessage {
    private Map<String, Object> headers = new HashMap<>();
    private String message;
    private String queueName;
    private String brokeUrl;

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getBrokeUrl() {
        return brokeUrl;
    }

    public void setBrokeUrl(String brokeUrl) {
        this.brokeUrl = brokeUrl;
    }
}
