package com.example;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.context.slf4j.MDCCurrentTraceContext;
import brave.propagation.B3Propagation;
import brave.propagation.ExtraFieldPropagation;
import com.jcfc.microservice.tracer.TracerManager;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.amqp.RabbitMQSender;

import java.util.concurrent.TimeUnit;

public class TraceDemo {

    public static void main(String[] args) {

        Tracer tracer = TracerManager.getTracing().tracer();
        Span span = tracer.newTrace().name("encode").start();
        try {
            doSomethingExpensive();
        } finally {
            span.finish();
        }


        Span twoPhase = tracer.newTrace().name("twoPhase").start();
        try {
            Span prepare = tracer.newChild(twoPhase.context()).name("prepare").start();
            try {
                prepare();
            } finally {
                prepare.finish();
            }
            Span commit = tracer.newChild(twoPhase.context()).name("commit").start();
            try {
                commit();
            } finally {
                commit.finish();
            }
        } finally {
            twoPhase.finish();
        }


        sleep(1000);

    }

    private static void doSomethingExpensive() {
        sleep(500);
    }

    private static void commit() {
        sleep(500);
    }

    private static void prepare() {
        sleep(500);
    }

    private static void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
