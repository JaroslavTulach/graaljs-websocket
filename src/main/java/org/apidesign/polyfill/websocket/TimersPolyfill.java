package org.apidesign.polyfill.websocket;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class TimersPolyfill {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
        var thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final ExecutorService executor;

    public TimersPolyfill(ExecutorService executor) {
        this.executor = executor;
    }

    public Object setTimeout(Consumer<Object[]> func, long delay, Object... args) {
        Executor delayedExecutor = CompletableFuture.delayedExecutor(delay, TIME_UNIT, executor);
        return CompletableFuture.runAsync(run(func, args), delayedExecutor);
    }

    public Object setInterval(Consumer<Object[]> func, long delay, Object... args) {
        return scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(run(func, args)), delay, delay, TIME_UNIT);
    }

    public void clearTimeout(Object actionId) {
        if (actionId instanceof Future action) {
            action.cancel(true);
        }
    }

    public void clearInterval(Object actionId) {
        clearTimeout(actionId);
    }

    private static Runnable run(Consumer<Object[]> func, Object[] arg) {
        return () -> {
            func.accept(arg);
        };
    }

}
