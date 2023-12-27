package org.apidesign.polyfill.websocket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Timers implements AutoCloseable {

    private static final CompletableFuture<Void> NULL_ACTION = CompletableFuture.completedFuture(null);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Map<UUID, Future<Void>> actions = new HashMap<>();

    private final ExecutorService executor;

    public Timers() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public Timers(ExecutorService executor) {
        this.executor = executor;
    }

    public UUID setTimeout(Consumer<Object[]> func, long delay, Object... args) {
        Executor delayedExecutor = CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, executor);
        CompletableFuture<Void> delayedAction = CompletableFuture.runAsync(run(func, args), delayedExecutor);

        return registerAction(delayedAction);
    }

    public UUID setInterval(Consumer<Object[]> func, long delay, Object... args) {
        Future<Void> scheduledAction = scheduleAtFixedRate(run(func, args), delay, TimeUnit.MILLISECONDS);

        return registerAction(scheduledAction);
    }

    public void clearTimeout(UUID actionId) {
        Future<Void> action = actions.getOrDefault(actionId, NULL_ACTION);
        action.cancel(true);
    }

    public void clearInterval(UUID actionId) {
        clearTimeout(actionId);
    }

    private Runnable run(Consumer<Object[]> func, Object[] arg) {
        return () -> {
            func.accept(arg);
        };
    }

    private Future<Void> scheduleAtFixedRate(Runnable r, long delay, TimeUnit unit) {
        return (Future<Void>) scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(r), delay, delay, unit);
    }

    private UUID registerAction(Future<Void> action) {
        UUID newActionId = UUID.randomUUID();
        actions.put(newActionId, action);

        return newActionId;
    }

    @Override
    public void close() {
        executor.close();
        scheduledExecutor.close();
    }
}
