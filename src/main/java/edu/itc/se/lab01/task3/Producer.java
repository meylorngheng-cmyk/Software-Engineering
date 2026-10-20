package edu.itc.se.lab01.task3;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {
    private static final int POISON_PILL_ID = -1;
    private final OrderQueue<Order> queue;
    private final int firstOrderId;
    private final int orderCount;
    private final int consumerCount;
    private final AtomicInteger producersRemaining;

    public Producer(OrderQueue<Order> queue, int firstOrderId, int orderCount,
                    int consumerCount, AtomicInteger producersRemaining) {
        this.queue = queue;
        this.firstOrderId = firstOrderId;
        this.orderCount = orderCount;
        this.consumerCount = consumerCount;
        this.producersRemaining = producersRemaining;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < orderCount; i++) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(21));
                int id = firstOrderId + i;
                int quantity = ThreadLocalRandom.current().nextInt(1, 6);
                queue.put(new Order(id, "item-" + id, quantity));
            }

            if (producersRemaining.decrementAndGet() == 0) {
                Order poisonPill = new Order(POISON_PILL_ID, "poison", 0);
                for (int i = 0; i < consumerCount; i++) {
                    queue.put(poisonPill);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}