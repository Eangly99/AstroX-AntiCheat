package dev.naruto.astrox.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class ObjectPool<T extends Poolable> {
    private final ThreadLocal<Deque<T>> local;
    private final Supplier<T> factory;
    private final int maxSize;

    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.max(16, maxSize);
        this.local = ThreadLocal.withInitial(ArrayDeque::new);
    }

    public T acquire() {
        Deque<T> deque = local.get();
        T obj = deque.pollFirst();
        return obj != null ? obj : factory.get();
    }

    public void release(T obj) {
        obj.reset();
        Deque<T> deque = local.get();
        if (deque.size() < maxSize) {
            deque.addFirst(obj);
        }
    }
}
