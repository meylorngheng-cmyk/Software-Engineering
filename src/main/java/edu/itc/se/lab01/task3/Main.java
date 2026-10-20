package edu.itc.se.lab01.task3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final int PRODUCER_COUNT = 2;
    private static final int ORDERS_PER_PRODUCER = 50;
    private static final int CONSUMER_COUNT = 3;
    private static final int BUFFER_CAPACITY = 5;

    public static void main(String[] args) throws InterruptedException {
        run(new BoundedBuffer<>(BUFFER_CAPACITY));
    }

    static void run(OrderQueue<Order> queue) throws InterruptedException {
        AtomicInteger processedCount = new AtomicInteger();
        AtomicInteger producersRemaining = new AtomicInteger(PRODUCER_COUNT);
        List<Thread> consumers = new ArrayList<>();
        List<Thread> producers = new ArrayList<>();
        long start = System.nanoTime();

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            Thread consumer = new Thread(
                    new Consumer("consumer-" + (i + 1), queue, processedCount));
            consumers.add(consumer);
            consumer.start();
        }
        for (int i = 0; i < PRODUCER_COUNT; i++) {
            int firstId = i * ORDERS_PER_PRODUCER;
            Thread producer = new Thread(new Producer(queue, firstId,
                    ORDERS_PER_PRODUCER, CONSUMER_COUNT, producersRemaining));
            producers.add(producer);
            producer.start();
        }

        for (Thread producer : producers) {
            producer.join();
        }
        for (Thread consumer : consumers) {
            consumer.join();
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        int expected = PRODUCER_COUNT * ORDERS_PER_PRODUCER;
        int processed = processedCount.get();
        System.out.printf("Total processed: %d (expected %d) in %d ms%n",
                processed, expected, elapsedMs);
        if (processed != expected) {
            throw new AssertionError("Expected " + expected + " processed orders, got " + processed);
        }
    }
}