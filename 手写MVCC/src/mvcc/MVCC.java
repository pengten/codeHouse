package mvcc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVCC�����߼�
 */
public class MVCC {

    // 1:��� 2�����ύ 3�����ظ��� 4������
    private static int level = 2;

    private List<Line> table = new ArrayList<>();

    private Map<Long, Set<Node>> transactionMap = new ConcurrentHashMap<>();

    /**
     * ��������
     *
     * @return ����ID
     */
    public long startTransaction() {
        return System.currentTimeMillis() + (long) (Math.random() * 1000);
    }

    /**
     * �ύ����
     *
     * @param transactionId ����ID
     */
    public void commit(long transactionId) {
        Set<Node> updateNodes = transactionMap.remove(transactionId);
        if (updateNodes == null) {
            return;
        }
        updateNodes.forEach(node -> {
            node.commit = true;
            node.line.node = node;
            node.line.updateId.set(0);
            synchronized (node.line.updateId) {
                node.line.updateId.notifyAll();
            }
        });
    }

    /**
     * ��ȡĳһ������<br>
     * read a line
     *
     * @param lineNum       ����(number of line)
     * @param transactionId ����ID(the id of transaction)
     * @return
     */
    public Object read(int lineNum, long transactionId) {
        if (lineNum >= table.size()) {
            return null;
        }
        switch (level) {
            case 1:
                break;
            case 2: {
                Node node = table.get(lineNum).node;
                Object value = null;
                while (node != null) {
                    if (node.isUpdate && node.commit) {
                        value = node.value;
                    }
                    if (node.isUpdate && node.transactionId == transactionId) {
                        value = node.value;
                        break;
                    }
                    node = node.next;
                }
                return value;
            }
            case 3:
                break;
            case 4:
                break;
            default:
                return -1;
        }
        return -1;
    }

    /**
     * ����ĳһ������<br>
     * update a line
     *
     * @param value         ����������(data of line)
     * @param lineNum       �к�(number of line)
     * @param transactionId ����ID(the id of transaction)
     * @return
     */
    public boolean update(Object value, int lineNum, long transactionId) {
        if (lineNum >= table.size()) {
            return false;
        }
        Line line = table.get(lineNum);
        long updateId = line.updateId.get();

        // ������������������Ի�ȡ����
        if (updateId != transactionId) {
            while (!line.updateId.compareAndSet(0, transactionId)) {
                try {
                    synchronized (line.updateId) {
                        line.updateId.wait(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Node node = flushMyNode(line.node, value, transactionId);

        // ��¼��ǰ������µ������У������ύ
        Set<Node> nodeList = transactionMap.computeIfAbsent(transactionId, k -> new HashSet<>());
        nodeList.add(node);

        return true;
    }

    /**
     * ������ع������ҵ���ǰ����ڵ㣬���Ҹ�������<br>
     * Locate the current transaction node in the transaction rollback chain and update the data
     *
     * @param node          �ع������(Rollback chain start point)
     * @param value         ������(the new data)
     * @param transactionId ����ID(id of the transaction)
     * @return
     */
    private Node flushMyNode(Node node, Object value, long transactionId) {
        synchronized (node.line) {
            Node tail = node;
            while (tail.next != null) {
                if (tail.isUpdate && tail.transactionId == transactionId) {
                    break;
                }
                tail = tail.next;
            }
            if (tail.transactionId == transactionId && tail.isUpdate) {
                tail.value = value;
            } else {
                Node newNode = new Node(transactionId, tail, null, value, false);
                newNode.isUpdate = true;
                newNode.line = tail.line;
                tail.next = newNode;
                tail = newNode;
            }
            return tail;
        }
    }

    /**
     * ��������<br>
     * insert data
     *
     * @param value         data
     * @param transactionId ����ID(id of the transaction)
     * @return
     */
    public synchronized int insert(Object value, long transactionId) {
        Node node = new Node(0, null, null, value, true);
        node.isUpdate = true;
        Line line = new Line();
        node.line = line;
        line.node = node;
        line.lineNum = table.size();
        table.add(line);
        return line.lineNum;
    }

    public void callback(long transactionId) {
        Set<Node> updateNodes = transactionMap.remove(transactionId);
        if (updateNodes == null) {
            return;
        }
        updateNodes.forEach(node -> {
            node.line.updateId.set(0);
            synchronized (node.line) {
                if (node.pre != null) {
                    node.pre.next = node.next;
                }
                if (node.next != null) {
                    node.next.pre = node.pre;
                }
            }
            synchronized (node.line.updateId) {
                node.line.updateId.notifyAll();
            }
        });
    }
}
