import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import static util.Print.*;


class PrioritizedTask implements Runnable, Comparable<PrioritizedTask> {
    private Random rand = new Random(47);
    private static int counter = 0;
    private final int id = counter++;
    private final int priority;
    public PrioritizedTask(int priority) {
        this.priority = priority;
    }

    public int compareTo(PrioritizedTask arg) {
        return priority < arg.priority ? 1 : (priority > arg.priority ? -1 : 0);
    }

    public void run() {
        print(this);
        System.out.println(Thread.currentThread().getName() + " id " + id + " is ended");
    }

    public String toString() {
        return String.format("[%1$-3d]", priority) + " Task " + id;
    }

    public static class EndSentinel extends PrioritizedTask {
        private ExecutorService exec;

        public EndSentinel(ExecutorService e) {
            super(-1); // Lowest priority in this program
            exec = e;
        }

        public void run() {
            print(this + " Calling shutdownNow()");
            exec.shutdownNow();
        }
    }
}

class PrioritizedTaskProducer implements Runnable {
    private Random rand = new Random(47);
    private Queue<Runnable> queue;
    private ExecutorService exec;

    public PrioritizedTaskProducer(Queue<Runnable> q, ExecutorService e) {
        queue = q;
        exec = e; // Used for EndSentinel
    }
    public void run() {
        // Unbounded queue; never blocks.
        // Fill it up fast with random priorities:
        for (int i = 0; i < 20; i++) { queue.add(new PrioritizedTask(rand.nextInt(100))); }

        queue.add(new PrioritizedTask.EndSentinel(exec));
        System.out.println("Finished PrioritizedTaskProducer"); // не успевает выполниться
    }
}

class PrioritizedTaskConsumer implements Runnable {
    private PriorityBlockingQueue<Runnable> q;
    public PrioritizedTaskConsumer( PriorityBlockingQueue<Runnable> q) {
        this.q = q;
    }

    public void run() {
        try {
            while (!Thread.interrupted())
                // Use CURRENT THREAD to run the task:
                q.take().run(); // ЗАПУСКАЕТ КАЖДОЕ ЗАДАНИЕ в СВОЕМ ПОТОКЕ ??? НЕТ - все в одном
        } catch (InterruptedException e) {
            // Acceptable way to exit
            System.out.println("Inerruped with SENTINEL");
        }
        print("Finished PrioritizedTaskConsumer");
    }
}

public class PriorityBlockingQueueDemo {
    public static void main(String[] args) throws Exception {
        Random rand = new Random(47);
        ExecutorService exec = Executors.newCachedThreadPool();
        PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>();

        exec.execute(new PrioritizedTaskProducer(queue, exec));
        exec.execute(new PrioritizedTaskConsumer(queue));
    }
} /* (Execute to see output) *///:~