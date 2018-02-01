package com.jcfc.microservice.tracer.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

/*
* 网络工具类
* 作者：姜敏
* 版本：V1.0
* 创建日期：2017/4/13
* 修改日期:2017/4/13
*/
public class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static final Map<String, String> HOST_NAME_CACHE = new LRUCache(10);
    private static volatile InetAddress LOCAL_ADDRESS = null;


    public static boolean isLocalHost(String host) {
        return host != null && (LOCAL_IP_PATTERN.matcher(host).matches() || host.equalsIgnoreCase("localhost"));
    }

    public static boolean isAnyHost(String host) {
        return "0.0.0.0".equals(host);
    }

    private static boolean isInvalidLocalHost(String host) {
        return host == null || host.length() == 0 || host.equalsIgnoreCase("localhost") || host.equals("0.0.0.0") || LOCAL_IP_PATTERN.matcher(host).matches();
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    private static boolean isValidAddress(InetAddress address) {
        if(address != null && !address.isLoopbackAddress()) {
            String name = address.getHostAddress();
            return name != null && !"0.0.0.0".equals(name) && !"127.0.0.1".equals(name) && IP_PATTERN.matcher(name).matches();
        } else {
            return false;
        }
    }

    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null?"127.0.0.1":address.getHostAddress();
    }

    private static InetAddress getLocalAddress() {
        if(LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        } else {
            InetAddress localAddress = getLocalAddress0();
            LOCAL_ADDRESS = localAddress;
            return localAddress;
        }
    }

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;

        try {
            localAddress = InetAddress.getLocalHost();
            if(isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable var6) {
            logger.warn("Failed to retriving ip address, " + var6.getMessage(), var6);
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if(interfaces != null) {
                while(interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = (NetworkInterface)interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if(addresses != null) {
                            while(addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = (InetAddress)addresses.nextElement();
                                    if(isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable var5) {
                                    logger.warn("Failed to retriving ip address, " + var5.getMessage(), var5);
                                }
                            }
                        }
                    } catch (Throwable var7) {
                        logger.warn("Failed to retriving ip address, " + var7.getMessage(), var7);
                    }
                }
            }
        } catch (Throwable var8) {
            logger.warn("Failed to retriving ip address, " + var8.getMessage(), var8);
        }

        logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }

    public static String getHostName(String address) {
        try {
            int i = address.indexOf(58);
            if(i > -1) {
                address = address.substring(0, i);
            }

            String hostname = (String) HOST_NAME_CACHE.get(address);
            if(hostname != null && hostname.length() > 0) {
                return hostname;
            }

            InetAddress inetAddress = InetAddress.getByName(address);
            if(inetAddress != null) {
                hostname = inetAddress.getHostName();
                HOST_NAME_CACHE.put(address, hostname);
                return hostname;
            }
        } catch (Throwable var4) {
            ;
        }

        return address;
    }

    public static String toURL(String protocol, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://");
        sb.append(host).append(':').append(port);
        if(path.charAt(0) != 47) {
            sb.append('/');
        }

        sb.append(path);
        return sb.toString();
    }

}
