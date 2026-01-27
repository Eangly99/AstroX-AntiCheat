package dev.naruto.astrox.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StripedExecutor {
    private final SerialExecutor[] stripes;

    public StripedExecutor(ExecutorService backend, int stripeCount) {
        int size = 1;
        while (size < stripeCount) {
            size <<= 1;
        }
        this.stripes = new SerialExecutor[size];
        for (int i = 0; i < stripes.length; i++) {
            stripes[i] = new SerialExecutor(backend);
        }
    }

    public void execute(long key, Runnable task) {
        int idx = (int) (key ^ (key >>> 32));
        idx &= (stripes.length - 1);
        stripes[idx].execute(task);
    }

    private static final class SerialExecutor implements Executor {
        private final Executor backend;
        private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean running = new AtomicBoolean(false);

        private SerialExecutor(Executor backend) {
            this.backend = backend;
        }

        @Override
        public void execute(Runnable command) {
            queue.add(command);
            if (running.compareAndSet(false, true)) {
                backend.execute(this::drain);
            }
        }

        private void drain() {
            try {
                Runnable task;
                while ((task = queue.poll()) != null) {
                    task.run();
                }
            } finally {
                running.set(false);
                if (!queue.isEmpty() && running.compareAndSet(false, true)) {
                    backend.execute(this::drain);
                }
            }
        }
    }
}
