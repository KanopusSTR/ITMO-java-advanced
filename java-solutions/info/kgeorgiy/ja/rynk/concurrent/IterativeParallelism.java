package info.kgeorgiy.ja.rynk.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IterativeParallelism implements ScalarIP {

    ParallelMapper mapper;

    public IterativeParallelism() {
        this.mapper = null;
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T maximum(int treadsCount, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return this.<T, T>getWithThreads(treadsCount, list,
                t -> t.max(comparator).orElseThrow(),
                t -> t.max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(int treadsCount, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(treadsCount, list, comparator.reversed());
    }


    @Override
    public <T> boolean all(int treadsCount, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return getWithThreads(treadsCount, list, t -> t.allMatch(predicate), t -> t.allMatch(x -> x));
    }

    @Override
    public <T> boolean any(int treadsCount, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return !all(treadsCount, list, predicate.negate());
    }


    public <T> int count(int treadsCount, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return getWithThreads(treadsCount, list,
                s -> (int) s.filter(predicate).count(),
                s -> s.mapToInt(Integer::intValue).sum());
    }

    private <T, J> J getWithThreads(int treadsCount, List<? extends T> list,
                                    Function<Stream<? extends T>, J> f1,
                                    Function<Stream<? extends J>, J> f2) throws InterruptedException {
        int count = Math.min(treadsCount, list.size());
        List<J> answerList = new ArrayList<>(Collections.nCopies(count, null));
        List<Thread> threads = new ArrayList<>();
        final int blockSize = list.size() / count;
        final int remainder = list.size() % count;
        List<Stream<? extends T>> tasks = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            final int bigBlockCount = Math.min(i, remainder);
            final int start = bigBlockCount + i * blockSize;
            tasks.add(list.subList(start, start + blockSize + (i < remainder ? 1 : 0)).stream());
        }
        if (mapper == null) {
            for (int i = 0; i < count; ++i) {
                Stream<? extends T> stream = tasks.get(i);
                final int threadNumber = i;
                Thread thread = new Thread(() -> answerList.set(threadNumber, f1.apply(stream)));
                thread.start();
                threads.add(thread);
            }
            InterruptedException e = new InterruptedException("Error while working with threads");
            boolean broken = false;
            for (Thread thread : threads) {
                if (!broken) {
                    try {
                        thread.join();
                    } catch (InterruptedException e1) {
                        broken = true;
                        e.addSuppressed(e1);
                        thread.interrupt();
                    }
                } else {
                    thread.interrupt();
                }
            }
            if (broken) {
                throw e;
            }
        } else {
            return f2.apply(mapper.map(f1, tasks).stream());
        }

        return f2.apply(answerList.stream());
    }
}
