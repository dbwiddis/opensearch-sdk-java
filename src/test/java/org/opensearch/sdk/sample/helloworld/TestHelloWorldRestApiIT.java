/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestClient;
import org.opensearch.client.WarningsHandler;
import org.opensearch.test.rest.OpenSearchRestTestCase;

public class TestHelloWorldRestApiIT extends OpenSearchRestTestCase {

    private static final String HELLOWORLD_MAIN_CLASS = HelloWorldExtension.class.getName();
    private static final String OPENSEARCH_MAIN_CLASS = "org.opensearch.bootstrap.OpenSearch";

    public static void main(String[] args) throws IOException, InterruptedException {

        boolean strictDeprecationMode = false;

        TestHelloWorldRestApiIT it = new TestHelloWorldRestApiIT();
        it.initClient();
        RestClient client = client();
        Request request = new Request("GET", "/hello");
        // headers.forEach(header -> options.addHeader(header.getName(), header.getValue()));
        RequestOptions.Builder options = RequestOptions.DEFAULT.toBuilder();
        options.setWarningsHandler(strictDeprecationMode ? WarningsHandler.STRICT : WarningsHandler.PERMISSIVE);
        request.setOptions(options.build());
        // params.entrySet().forEach(it -> request.addParameter(it.getKey(), it.getValue()));
        // request.setEntity(entity); }
        Response response = client.performRequest(request);
        System.out.println(response.toString());

        // We will need three threads, executed in order:
        // 1 - Start extension
        // 2 - Start opensearch
        // 3 - Run tests against Opensearch (maybe can include in 2)

        ExecutorService executorService = Executors.newFixedThreadPool(3);

        CountDownLatch latch = new CountDownLatch(1);

        HelloWorldThread hwt = new HelloWorldThread(latch);
        executorService.execute(hwt);
        latch.await(10, TimeUnit.SECONDS);
        System.out.println("Ready for next thread");
        Thread.sleep(10000);

        latch = new CountDownLatch(1);
        // ERRORS< need to set HOME_PATH, see Environment.java
        // ERROR: the system property [opensearch.path.conf] must be set
        OpenSearchThread ost = new OpenSearchThread(latch);
        executorService.execute(ost);
        latch.await(10, TimeUnit.SECONDS);
        System.out.println("Executing command line...");

        for (String s : ExecuteCommandLine.exec("pwd")) {
            System.out.println(s);
        }
        Thread.sleep(10000);

        for (String s : ExecuteCommandLine.exec("curl -X GET localhost:9200/_extensions/_opensearch-sdk-java-1/hello")) {
            System.out.println(s);
        }
        Thread.sleep(10000);

        hwt.interrupt();
        ost.interrupt();
        executorService.shutdown();
        System.out.println("All done!");

    }

    private static class HelloWorldThread implements Runnable {

        private CountDownLatch latch;
        private AtomicBoolean running = new AtomicBoolean(false);
        private Process process = null;

        public HelloWorldThread(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            System.out.println("Starting " + this.getClass().getName());

            String classPath = System.getProperty("java.class.path");
            String root = classPath.split(":")[0];
            String java = String.join(File.separator, System.getProperty("java.home"), "bin", "java");
            List<String> execExtension = List.of(java, "-cp", classPath, HELLOWORLD_MAIN_CLASS);

            try {
                process = new ProcessBuilder(execExtension).inheritIO().start();
                System.out.println("Initialized " + HELLOWORLD_MAIN_CLASS);
                running.set(true);
                latch.countDown();
                while (running.get()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException e) {
                latch.countDown();
            }
        }

        public void interrupt() {
            running.set(false);
            process.destroy();
            System.out.println("Goodbye, cruel world!");
        }
    }

    private static class OpenSearchThread implements Runnable {

        private CountDownLatch latch;
        private AtomicBoolean running = new AtomicBoolean(false);
        private Process process = null;

        public OpenSearchThread(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            System.out.println("Starting " + this.getClass().getName());

            String classPath = System.getProperty("java.class.path");
            String java = String.join(File.separator, System.getProperty("java.home"), "bin", "java");
            List<String> execOpenSearch = List.of(java, "-cp", classPath, OPENSEARCH_MAIN_CLASS);

            try {
                process = new ProcessBuilder(execOpenSearch).inheritIO().start();
                System.out.println("Initialized " + OPENSEARCH_MAIN_CLASS);
                running.set(true);
                latch.countDown();
                while (running.get()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException e) {
                latch.countDown();
            }
        }

        public void interrupt() {
            running.set(false);
            process.destroy();
            System.out.println("OpenSearch is now ClosedSearch.");
        }
    }
}
