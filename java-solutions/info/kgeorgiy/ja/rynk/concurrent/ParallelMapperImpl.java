package info.kgeorgiy.ja.rynk.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads = new ArrayList<>();
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    public ParallelMapperImpl(int threadsCount) {
        Runnable thread = (() -> {
            try {
                while (!Thread.interrupted()) {
                    Runnable task;
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    if (task != null) {
                        task.run();
                    }
                }
            } catch (InterruptedException ignored) {
            }
        });
        for (int i = 0; i < threadsCount; ++i) {
            threads.add(new Thread(thread));
            threads.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> answerList = new ArrayList<>(Collections.nCopies(args.size(), null));
        Counter counter = new Counter();
        List<RuntimeException> exceptions = new ArrayList<>(Collections.nCopies(args.size(), null));
        for (int i = 0; i < args.size(); ++i) {
            int argNumber = i;
            Runnable task = () -> {
                try {
                    answerList.set(argNumber, f.apply(args.get(argNumber)));
                } catch (RuntimeException e) {
                    synchronized (exceptions) {
                        exceptions.set(argNumber, e);
                    }
                } finally {
                    synchronized (counter) {
                        counter.count++;
                        counter.notify();
                    }
                }
            };
            synchronized (tasks) {
                tasks.add(task);
                tasks.notify();
            }
        }

        synchronized (counter) {
            while (counter.count != args.size()) {
                counter.wait();
            }
        }
        RuntimeException exception = null;
        for (RuntimeException exception1 : exceptions) {
            if (exception1 != null) {
                if (exception == null) {
                    exception = exception1;
                } else {
                    exception.addSuppressed(exception1);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return answerList;
    }

    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e1) {
                thread.interrupt();
            }
        }
    }

    private static class Counter {
        private Counter() {
            count = 0;
        }

        private int count;
    }
}
