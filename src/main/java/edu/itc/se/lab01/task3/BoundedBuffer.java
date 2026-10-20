package edu.itc.se.lab01.task3;

import java.util.ArrayDeque;
import java.util.Objects;

public class BoundedBuffer<T> implements OrderQueue<T> {
    private final ArrayDeque<T> items = new ArrayDeque<>();
    private final int capacity;

    public BoundedBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    @Override
    public synchronized void put(T item) throws InterruptedException {
        Objects.requireNonNull(item, "item");
        while (items.size() == capacity) {
            wait();
        }
        items.addLast(item);
        notifyAll();
    }

    @Override
    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T item = items.removeFirst();
        notifyAll();
        return item;
    }
}