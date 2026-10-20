package edu.itc.se.lab01.task3;

public interface OrderQueue<T> {
    void put(T item) throws InterruptedException;

    T take() throws InterruptedException;
}