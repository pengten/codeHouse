package mvcc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVCC核心逻辑
 */
public class MVCC {

    // 1:脏读 2：读提交 3：可重复读 4：串行
    private static int level = 2;

    private List<Line> table = new ArrayList<>();

    private Map<Long, Set<Node>> transactionMap = new ConcurrentHashMap<>();

    /**
     * 创建事务
     *
     * @return 事务ID
     */
    public long startTransaction() {
        return System.currentTimeMillis() + (long) (Math.random() * 1000);
    }

    /**
     * 提交事务
     *
     * @param transactionId 事务ID
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
     * 读取某一行数据<br>
     * read a line
     *
     * @param lineNum       行数(number of line)
     * @param transactionId 事务ID(the id of transaction)
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
     * 更新某一行数据<br>
     * update a line
     *
     * @param value         该行新数据(data of line)
     * @param lineNum       行号(number of line)
     * @param transactionId 事务ID(the id of transaction)
     * @return
     */
    public boolean update(Object value, int lineNum, long transactionId) {
        if (lineNum >= table.size()) {
            return false;
        }
        Line line = table.get(lineNum);
        long updateId = line.updateId.get();

        // 如果不持有行锁，则尝试获取行锁
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

        // 记录当前事务更新的所有行，便于提交
        Set<Node> nodeList = transactionMap.computeIfAbsent(transactionId, k -> new HashSet<>());
        nodeList.add(node);

        return true;
    }

    /**
     * 在事务回滚链中找到当前事务节点，并且更新数据<br>
     * Locate the current transaction node in the transaction rollback chain and update the data
     *
     * @param node          回滚链起点(Rollback chain start point)
     * @param value         新数据(the new data)
     * @param transactionId 事务ID(id of the transaction)
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
     * 插入数据<br>
     * insert data
     *
     * @param value         data
     * @param transactionId 事务ID(id of the transaction)
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
