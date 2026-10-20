# Lab 01 - Multithreading

This project uses JDK 25 and Maven. Verify a clean build with:

```powershell
mvn -q clean verify
```

## Task 1: Threads and interruption

```powershell
java -cp target\classes edu.itc.se.lab01.task1.main
```

## Task 2: Shared counters

```powershell
java -cp target\classes edu.itc.se.lab01.task2.Bench
```

The benchmark compares an unsafe counter with synchronized, lock-based,
atomic, and `LongAdder` implementations.

## Task 3: Producer and consumer queues

```powershell
java -cp target\classes edu.itc.se.lab01.task3.Main
java -cp target\classes edu.itc.se.lab01.task3.Main2
```

The examples compare a guarded bounded queue and `ArrayBlockingQueue`.

## Task 4: Executors and virtual threads

```powershell
java -cp target\classes edu.itc.se.lab01.task4.Main
```

This compares CPU-bound tasks and blocking tasks using platform and virtual
threads.

## Task 5: Deadlocks and lock management

```powershell
java -cp target\classes edu.itc.se.lab01.task5.Bank deadlock
```

The deadlock mode is intentional; the watchdog reports detected deadlocks.
Stop it with Ctrl+C. Other modes demonstrate ordered locking, `tryLock`, and
graceful shutdown:

```powershell
java -cp target\classes edu.itc.se.lab01.task5.Bank ordered
java -cp target\classes edu.itc.se.lab01.task5.Bank trylock
java -cp target\classes edu.itc.se.lab01.task5.Bank shutdown-ordered
java -cp target\classes edu.itc.se.lab01.task5.Bank shutdown-trylock
```

The watchdog terminal capture is in [evidence/deadlock-detection.txt](evidence/deadlock-detection.txt).

Each task is recorded in its own commit on the `lab-01` branch.
