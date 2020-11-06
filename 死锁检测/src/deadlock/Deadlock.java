package deadlock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ����������
 *
 * @author yangwenpeng
 * @version 2020��11��6��10:07:37
 */
public class Deadlock<T, E> {

    private Map<E, T> resourceTable = new ConcurrentHashMap<>();

    private Map<T, E> waitTable = new ConcurrentHashMap<>();

    /**
     * ���Ի�ȡ����Դ
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
         * �����������
         */
        DEAD,
        /**
         * ���Ի�ȡ���ȴ���Դ�С���
         */
        WAIT,
        /**
         * ��ȡ�ɹ�
         */
        OK,
        /**
         * ��������Դ���ھ�����ͬʱֻ�ܾ���һ����Դ
         */
        WAITED
    }
}
