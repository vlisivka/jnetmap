package ch.rakudave.jnetmap.controller;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * A stripped-down version of the ScheduledExecutorService
 *
 * @author rakudave
 * @see ScheduledExecutorService
 */
public class Scheduler {
    static ScheduledExecutorService executor;

    public static void execute(Runnable command) {
        executor.execute(command);
    }

    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    public static <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    public static <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    public static <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return executor.schedule(callable, delay, unit);
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return executor.schedule(command, delay, unit);
    }

    public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public static ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public static Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    public static <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }
}
