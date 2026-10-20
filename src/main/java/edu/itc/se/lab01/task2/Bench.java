package edu.itc.se.lab01.task2;


import java.util.ArrayList;
import java.util.List;

public class Bench {
    static final int THREADS = 8;
    static final int ITERATIONS = 1_000_000;

    static void run(String label, Counter counter) throws InterruptedException {
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) counter.increment();
            }, "inc-" + t);
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        long expected = (long) THREADS * ITERATIONS;
        long value = counter.get();
        System.out.printf("%-22s value=%,d  correct=%-5b  %d ms%n",
                label, value, value == expected, elapsedMs);
    }

    public static void main(String[] args) throws InterruptedException {
        // Observed unsafe results: 1,033,515 and 2,691,032 (third run: 8,000,000).
        for (int run = 1; run <= 3; run++) {
            run("UnsafeCounter run " + run, new UnsafeCounter());
        }
        run("SynchronizedCounter", new SynchronizedCounter());
        run("LockCounter", new LockCounter());
        run("AtomicCounter", new AtomicCounter());
        run("LongAdderCounter", new LongAdderCounter());
    }
}
