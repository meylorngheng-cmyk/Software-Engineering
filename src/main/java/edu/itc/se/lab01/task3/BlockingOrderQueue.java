package edu.itc.se.lab01.task3;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

final class BlockingOrderQueue<T> implements OrderQueue<T> {
    private final BlockingQueue<T> queue;

    BlockingOrderQueue(int capacity) {
        queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public void put(T item) throws InterruptedException {
        queue.put(item);
    }

    @Override
    public T take() throws InterruptedException {
        return queue.take();
    }
}