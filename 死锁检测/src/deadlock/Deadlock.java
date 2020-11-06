package deadlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 死锁核心类
 *
 * @author yangwenpeng
 * @version 2020年11月6日10:07:37
 */
public class Deadlock<T, E> {

    private Map<E, T> resourceTable = new ConcurrentHashMap<>();

    private Map<T, E> waitTable = new ConcurrentHashMap<>();

    /**
     * 尝试获取锁资源
     * @param waiter
     * @param resource
     * @return
     */
    public Status tryLock(T waiter, E resource) {
        T locked;
        synchronized (resource) {
            locked = resourceTable.get(resource);
            if (waiter.equals(locked)) {
                return Status.OK;
            }
            if (locked == null) {
                resourceTable.put(resource, waiter);
                waitTable.remove(waiter, resource);
                return Status.OK;
            }
        }
        while (locked != null) {
            E r = waitTable.get(locked);
            if (r == null) {
                synchronized (waiter) {
                    r = waitTable.get(waiter);
                    if (resource.equals(r)) {
                        return Status.WAIT;
                    }
                    if (r != null) {
                        return Status.WAITED;
                    }
                    waitTable.put(waiter, resource);
                }
                return Status.WAIT;
            }
            locked = resourceTable.get(r);
            if (waiter.equals(locked)) {
                return Status.DEAD;
            }
        }
        synchronized (waiter) {
            if (waitTable.containsKey(waiter)) {
                return Status.WAITED;
            }
            waitTable.put(waiter, resource);
        }
        return Status.WAIT;
    }


    public boolean release(T waiter, E resource) {
        return resourceTable.remove(resource, waiter);
    }

    public enum Status {
        /**
         * 将会造成死锁
         */
        DEAD,
        /**
         * 可以获取，等待资源中。。
         */
        WAIT,
        /**
         * 获取成功
         */
        OK,
        /**
         * 有其他资源正在竞争，同时只能竞争一个资源
         */
        WAITED
    }
}
