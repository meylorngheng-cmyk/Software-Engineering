package edu.itc.se.lab01.task4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class Main {
    private static Callable<Long> PrimeCounter(long from, long to) {
        return () -> countPrimes(from, to);
    }

    private static long countPrimes(long from, long to) {
        long count = 0;
        for (long n = Math.max(from, 2); n < to; n++) {
            if (isPrime(n)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        if (n % 2 == 0) {
            return n == 2;
        }
        for (long divisor = 3; divisor <= n / divisor; divisor += 2) {
            if (n % divisor == 0) {
                return false;
            }
        }
        return true;
    }

    private static Callable<String> FakeDownload(String url) {
        return FakeDownload(url, 200);
    }

    private static Callable<String> FakeDownload(String url, long sleepMillis) {
        return () -> {
            Thread.sleep(sleepMillis);
            return url + " -> " + url.length();
        };
    }

    private static Callable<String> downloadTask(String url, int index) {
        return () -> {
            if ((index + 1) % 50 == 0) {
                throw new IOException("boom");
            }
            return (index == 0 ? FakeDownload(url, 5_000) : FakeDownload(url)).call();
        };
    }

    private static void runDownloadBenchmark(
            String label, Supplier<ExecutorService> executorFactory) throws InterruptedException {
        long startedAt = System.nanoTime();
        int ok = 0;
        int failed = 0;
        int timedOut = 0;

        try (ExecutorService executor = executorFactory.get()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                futures.add(executor.submit(downloadTask("https://example.com/" + i, i)));
            }

            try {
                futures.get(0).get(1, TimeUnit.SECONDS);
                ok++;
            } catch (TimeoutException e) {
                futures.get(0).cancel(true);
                timedOut++;
            } catch (ExecutionException e) {
                failed++;
            }

            for (int i = 1; i < futures.size(); i++) {
                try {
                    futures.get(i).get();
                    ok++;
                } catch (ExecutionException e) {
                    failed++;
                }
            }
        }

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        System.out.printf("%s: ok=%d failed=%d timedOut=%d in %d ms%n",
                label, ok, failed, timedOut, elapsedMillis);
    }

    private static void runCompletableFutureExample() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> downloads = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                final int index = i;
                CompletableFuture<String> download = CompletableFuture
                        .supplyAsync(() -> {
                            try {
                                return downloadTask("https://example.com/" + index, index).call();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }, executor)
                        .handle((result, error) -> error == null
                                ? result
                                : "download failed: " + index);
                downloads.add(download);
            }

            CompletableFuture.allOf(downloads.toArray(CompletableFuture[]::new)).join();
            long fallbackCount = downloads.stream()
                    .map(CompletableFuture::join)
                    .filter(result -> result.startsWith("download failed:"))
                    .count();
            System.out.printf("CompletableFuture downloads: %d completed, %d fallback results%n",
                    downloads.size(), fallbackCount);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final long limit = 4_000_000;
        final int chunks = 16;

        long startedAt = System.nanoTime();
        long singleThreaded = countPrimes(0, limit);
        long singleElapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        System.out.printf("single-threaded: %d primes in %d ms%n",
                singleThreaded, singleElapsedMillis);

        startedAt = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors())) {
            List<Future<Long>> futures = new ArrayList<>();
            long chunkSize = limit / chunks;
            for (int i = 0; i < chunks; i++) {
                long from = i * chunkSize;
                long to = i == chunks - 1 ? limit : from + chunkSize;
                futures.add(pool.submit(PrimeCounter(from, to)));
            }

            long total = 0;
            for (Future<Long> future : futures) {
                try {
                    total += future.get();
                } catch (ExecutionException e) {
                    throw new IllegalStateException("Prime-counting task failed", e.getCause());
                }
            }
            long poolElapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            System.out.printf("pool (%d threads): %d primes in %d ms%n",
                    Runtime.getRuntime().availableProcessors(), total, poolElapsedMillis);
        }

        runDownloadBenchmark("downloads on fixed(20) platform threads",
                () -> Executors.newFixedThreadPool(20));
        runDownloadBenchmark("downloads on virtual threads",
                Executors::newVirtualThreadPerTaskExecutor);

        runCompletableFutureExample();
    }
}
