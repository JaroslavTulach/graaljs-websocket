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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class TimersPolyfill {

    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final CompletableFuture<Void> NULL_ACTION = CompletableFuture.completedFuture(null);

    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor((r) -> {
        var thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Future<Void>> actions = new HashMap<>();

    private final ExecutorService executor;

    public TimersPolyfill(ExecutorService executor) {
        this.executor = executor;
    }

    public UUID setTimeout(Consumer<Object[]> func, long delay, Object... args) {
        Executor delayedExecutor = CompletableFuture.delayedExecutor(delay, TIME_UNIT, executor);
        CompletableFuture<Void> delayedAction = CompletableFuture.runAsync(run(func, args), delayedExecutor);

        return registerAction(delayedAction);
    }

    public UUID setInterval(Consumer<Object[]> func, long delay, Object... args) {
        ScheduledFuture<Void> scheduledAction = scheduleAtFixedRate(run(func, args), delay);

        return registerAction(scheduledAction);
    }

    public void clearTimeout(UUID actionId) {
        Future<Void> action = actions.getOrDefault(actionId, NULL_ACTION);
        action.cancel(true);
        actions.remove(actionId);
    }

    public void clearInterval(UUID actionId) {
        clearTimeout(actionId);
    }

    private static Runnable run(Consumer<Object[]> func, Object[] arg) {
        return () -> {
            func.accept(arg);
        };
    }

    private ScheduledFuture<Void> scheduleAtFixedRate(Runnable r, long delay) {
        return (ScheduledFuture<Void>) scheduledExecutor.scheduleAtFixedRate(() -> executor.execute(r), delay, delay, TIME_UNIT);
    }

    private UUID registerAction(CompletableFuture<Void> action) {
        UUID actionId = UUID.randomUUID();
        actions.put(actionId, action.whenCompleteAsync((v, t) -> actions.remove(actionId), executor));

        return actionId;
    }

    private UUID registerAction(ScheduledFuture<Void> action) {
        UUID actionId = UUID.randomUUID();
        actions.put(actionId, action);

        return actionId;
    }

}
