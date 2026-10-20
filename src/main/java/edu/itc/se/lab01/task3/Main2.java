package edu.itc.se.lab01.task3;

public class Main2 {
    private static final int BUFFER_CAPACITY = 5;

    public static void main(String[] args) throws InterruptedException {
        Main.run(new BlockingOrderQueue<>(BUFFER_CAPACITY));
    }
}