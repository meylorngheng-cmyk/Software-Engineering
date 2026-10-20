package edu.itc.se.lab01.task5;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {
    private static final int ACCOUNT_COUNT = 4;
    private static final long INITIAL_BALANCE = 1_000;
    private static final int WORKER_COUNT = 8;
    private static final int TRANSFERS_PER_WORKER = 100_000;
    private static final long EXPECTED_TOTAL = ACCOUNT_COUNT * INITIAL_BALANCE;

    static final class Account {
        final int id;
        private long balance;

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }

        long balance() {
            return balance;
        }

        void withdraw(long amount) {
            balance -= amount;
        }

        void deposit(long amount) {
            balance += amount;
        }
    }

    private static final class LockedAccount {
        final Account account;
        final ReentrantLock lock = new ReentrantLock();

        LockedAccount(Account account) {
            this.account = account;
        }
    }

    @FunctionalInterface
    private interface Transfer {
        void apply(Account from, Account to, long amount) throws InterruptedException;
    }

    private static final class TryLockBank {
        private final LockedAccount[] accounts;
        private final AtomicLong retries = new AtomicLong();

        TryLockBank(Account[] accounts) {
            this.accounts = new LockedAccount[accounts.length];
            for (int i = 0; i < accounts.length; i++) {
                this.accounts[i] = new LockedAccount(accounts[i]);
            }
        }

        void transfer(Account from, Account to, long amount) throws InterruptedException {
            if (from == to) {
                return;
            }
            LockedAccount source = accounts[from.id];
            LockedAccount destination = accounts[to.id];
            while (true) {
                boolean sourceLocked = source.lock.tryLock();
                if (!sourceLocked) {
                    retryWithBackoff();
                    continue;
                }

                try {
                    if (destination.lock.tryLock()) {
                        try {
                            if (from.balance() >= amount) {
                                from.withdraw(amount);
                                to.deposit(amount);
                            }
                            return;
                        } finally {
                            destination.lock.unlock();
                        }
                    }
                } finally {
                    source.lock.unlock();
                }
                retryWithBackoff();
            }
        }

        long retries() {
            return retries.get();
        }

        private void retryWithBackoff() throws InterruptedException {
            retries.incrementAndGet();
            Thread.sleep(0, ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        }
    }

    private static void transfer(Account from, Account to, long amount) {
        synchronized (from) {
            synchronized (to) {
                if (from.balance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }

    private static void orderedTransfer(Account from, Account to, long amount) {
        if (from == to) {
            return;
        }
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            synchronized (second) {
                if (from.balance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            }
        }
    }

    private static Account[] newAccounts() {
        Account[] accounts = new Account[ACCOUNT_COUNT];
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = new Account(i, INITIAL_BALANCE);
        }
        return accounts;
    }

    private static long totalBalance(Account[] accounts) {
        long total = 0;
        for (Account account : accounts) {
            total += account.balance();
        }
        return total;
    }

    private static void startDeadlockWatchdog() {
        Thread watchdog = new Thread(() -> {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                long[] deadlockedIds = threadBean.findDeadlockedThreads();
                if (deadlockedIds != null) {
                    ThreadInfo[] deadlockedThreads = threadBean.getThreadInfo(deadlockedIds);
                    StringBuilder names = new StringBuilder();
                    for (ThreadInfo info : deadlockedThreads) {
                        if (names.length() > 0) {
                            names.append(", ");
                        }
                        names.append(info.getThreadName());
                    }
                    System.out.println("[watchdog] DEADLOCK detected between: " + names);
                }
            }
        }, "deadlock-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void runDeadlockDemo() {
        Account[] accounts = newAccounts();
        startDeadlockWatchdog();
        for (int worker = 0; worker < WORKER_COUNT; worker++) {
            Thread thread = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                while (true) {
                    Account from = accounts[random.nextInt(accounts.length)];
                    Account to = accounts[random.nextInt(accounts.length)];
                    if (from != to) {
                        transfer(from, to, random.nextLong(1, 50));
                    }
                }
            }, "transfer-" + worker);
            thread.start();
        }
    }

    private static void runFiniteBenchmark(
            String label, Account[] accounts, Transfer transfer, java.util.function.LongSupplier retries)
            throws InterruptedException {
        AtomicLong completed = new AtomicLong();
        long startedAt = System.nanoTime();
        Thread[] workers = new Thread[WORKER_COUNT];

        for (int worker = 0; worker < workers.length; worker++) {
            workers[worker] = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                    Account from = accounts[random.nextInt(accounts.length)];
                    int toIndex = random.nextInt(accounts.length - 1);
                    if (toIndex >= from.id) {
                        toIndex++;
                    }
                    try {
                        transfer.apply(from, accounts[toIndex], random.nextLong(1, 50));
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "transfer-" + worker);
            workers[worker].start();
        }

        joinAll(workers);
        long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startedAt);
        String retrySummary = label.equals("TryLockBank") ? " retries=" + retries.getAsLong() : "";
        System.out.printf("%s: transfers=%d%s total=%d (expected %d) in %d ms%n",
                label, completed.get(), retrySummary, totalBalance(accounts), EXPECTED_TOTAL,
                elapsedMillis);
    }

    private static void runTryLockBenchmark() throws InterruptedException {
        Account[] accounts = newAccounts();
        TryLockBank bank = new TryLockBank(accounts);
        runFiniteBenchmark("TryLockBank", accounts, bank::transfer, bank::retries);
    }

    private static void runGracefulShutdown(String label, Transfer transfer)
            throws InterruptedException {
        Account[] accounts = newAccounts();
        AtomicLong[] completed = new AtomicLong[WORKER_COUNT];
        Thread[] workers = new Thread[WORKER_COUNT];
        for (int worker = 0; worker < workers.length; worker++) {
            final int workerId = worker;
            completed[worker] = new AtomicLong();
            workers[worker] = new Thread(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Account from = accounts[random.nextInt(accounts.length)];
                        int toIndex = random.nextInt(accounts.length - 1);
                        if (toIndex >= from.id) {
                            toIndex++;
                        }
                        transfer.apply(from, accounts[toIndex], random.nextLong(1, 50));
                        completed[workerId].incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.printf("%s stopped after %d transfers%n",
                            Thread.currentThread().getName(), completed[workerId].get());
                }
            }, "transfer-" + worker);
            workers[worker].start();
        }

        Thread.sleep(2_000);
        System.out.printf("[main] interrupting %d workers after 2000 ms%n", workers.length);
        for (Thread worker : workers) {
            worker.interrupt();
        }
        for (Thread worker : workers) {
            worker.join(1_000);
        }

        long totalTransfers = 0;
        boolean allStopped = true;
        for (int i = 0; i < workers.length; i++) {
            totalTransfers += completed[i].get();
            allStopped &= !workers[i].isAlive();
        }
        System.out.printf("all workers stopped=%s transfers=%d total=%d (expected %d)%n",
                allStopped, totalTransfers, totalBalance(accounts), EXPECTED_TOTAL);
    }

    private static void joinAll(Thread[] workers) throws InterruptedException {
        for (Thread worker : workers) {
            worker.join();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length == 0 ? "deadlock" : args[0];
        switch (mode) {
            case "deadlock" -> runDeadlockDemo();
            case "ordered" -> runFiniteBenchmark(
                    "OrderedBank", newAccounts(), Bank::orderedTransfer, () -> 0);
            case "trylock" -> runTryLockBenchmark();
            case "shutdown-ordered" -> runGracefulShutdown("OrderedBank", Bank::orderedTransfer);
            case "shutdown-trylock" -> {
                Account[] accounts = newAccounts();
                TryLockBank bank = new TryLockBank(accounts);
                runGracefulShutdown("TryLockBank", bank::transfer);
            }
            default -> throw new IllegalArgumentException(
                    "Use one of: deadlock, ordered, trylock, shutdown-ordered, shutdown-trylock");
        }
    }
}
