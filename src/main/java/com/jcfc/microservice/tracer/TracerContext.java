package com.jcfc.microservice.tracer;

/**
 * Created by zhangjinpeng on 2018/8/10.
 */

public class TracerContext {
    private String serverName = "tracer-server";//应用名
    private String context;//应用名
    private String percentage = "1.0";//采样率
    private String addresses;//MQ地址
    private String userName;//MQ用户名
    private String password;//MQ密码

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getPercentage() {
        return percentage;
    }

    public void setPercentage(String percentage) {
        this.percentage = percentage;
    }

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(String addresses) {
        this.addresses = addresses;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
