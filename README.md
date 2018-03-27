# microservice-tracer
分布式服务调用链客户端

## 前言(preface)
微服务环境下的服务治理依赖于分布式服务跟踪系统，晋商消费金融的分布式服务跟踪系统建立在开源系统zipkin上，其中收集的数据存储在elasticsearch上，支持使用Kibana进行展示和搜索，同时，zipkin有自己的UI展示，用来展示跨系统的分布式调用链。
要建立起分布式调用链，服务端依赖于客户端将各自的调用上下文发送给服务端，zipkin支持多种数据收集方式，晋消的分布式服务跟踪系统要求通过rabbitmq来收集消息。

## 架构(architecture)
zipkin主要涉及五个组件 reporter collector storage search web UI 
*  Reporter发送从各service上收集的数据
*  Collector接收各service传输的数据
*  Storage可以是es，也可以是mysql等，默认存储在内存中
*  Query负责查询Storage中存储的数据,提供简单的JSON API获取数据，主要提供给web UI使用
*  Web 提供简单的web界面

## 接入方式（develop） 
#### 1.在需要收集跟踪点的系统中加入依赖的jar包 

#### 2.增加tracer.properties配置文件 

```
##应用名称，按实际填
tracer.server.name=tracer-demo 
##线程上下文模式：log4j|log4j2|slf4j，不配置的话是默认的线程上下文，不能打印traceId到日志文件
tracer.context.name=slf4j 
##采样率，1.0表示全部采样
tracer.sampler.percentage=1.0 
 
##发送器通过rabbitmq，配置地址
zipkin.sender.rabbitmq.addresses=20.4.17.26:5672,20.4.17.27:5672 
zipkin.sender.rabbitmq.username=zipkin 
zipkin.sender.rabbitmq.password=zipkin123 
```

#### 3.增加服务跟踪filter
>现只支持HTTP服务端、DUBBO、AOP三种方式 

HTTP服务端： 
    在web.xml中增加filter，如果只想拦截部分url，则修改url-pattern
```
<filter>
<filter-name>TracingFilter</filter-name>
<filter-class>com.jcfc.microservice.tracer.http.HttpTracingFilter</filter-class>
</filter>
<filter-mapping>
<filter-name>TracingFilter</filter-name>
<url-pattern>/*</url-pattern>
</filter-mapping>
```
DUBBO： 
    在dubbo的xml配置文件中添加filter，consumer、provider各添加各自的 
```
<dubbo:consumer filter="dubboConsumerTracingFilter" /> 
<dubbo:provider filter="dubboProviderTracingFilter" /> 
```
AOP： 
在spring的xml配置文件中添加aop切面相关配置，如下：
```
<!-- 切面类 -->
<bean id="tracingAspect" class="com.jcfc.microservice.tracer.aop.AopTracingFilter"/>
<!-- aop配置 可以配置多个-->
<aop:config>
    <!-- 切面类 其中pointcut里面的表达式根据业务自定义-->
    <aop:aspect id="aspectTracing" ref="tracingAspect">
        <aop:around method="around" pointcut="execution(* com.*.*Controller.*(..))"/>
    </aop:aspect>
</aop:config>
```
>>注：aop的方式不存在跨应用的调用链，可以用来分析内部复杂业务逻辑场景，不建议在dao层使用，尽量在业务层使用

HTTP客户端：原生的http调用方式，参考test里的demo

rabbitmq：提供了RabbitmqTracingHandler

