package edu.itc.se.lab01.task1;

public class CountdownTask implements Runnable {
    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        for (int count = 3; count >= 1; count--) {
            System.out.println(name + ": " + count);
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println(name + ": liftoff!");
    }
}
