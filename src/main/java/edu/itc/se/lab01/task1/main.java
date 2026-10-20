package edu.itc.se.lab01.task1;

// start() creates a brand-new thread and calls run() on it asynchronously.
// run() is just a normal method call, so in sequential mode we execute it in the
// current thread and do not get true concurrency.
public class main {
    public static void main(String[] args) throws InterruptedException {
        boolean sequential = args.length > 0 && "--sequential".equals(args[0]);

        Countdown apollo = new Countdown("apollo");
        Thread gemini = new Thread(new CountdownTask(), "gemini");
        Thread mercury = new Thread(() -> {
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
        }, "mercury");
        Thread artemis = Thread.ofVirtual().name("artemis").unstarted(new CountdownTask());

        if (sequential) {
            apollo.run();
            gemini.run();
            mercury.run();
            artemis.run();
        } else {
            apollo.start();
            gemini.start();
            mercury.start();
            artemis.start();
        }

        System.out.println("Mission control: all launched");

        apollo.join();
        gemini.join();
        mercury.join();
        artemis.join();

        System.out.println("Mission control: all landed");

        Thread[] threads = {apollo, gemini, mercury, artemis};
        for (Thread thread : threads) {
            System.out.println(thread.getName() + ": isVirtual=" + thread.isVirtual() + ", state=" + thread.getState());
        }
    }
}
