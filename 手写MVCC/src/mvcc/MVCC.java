package mvcc;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MVCC {

    // 1:脏读 2：读提交 3：可重复读 4：串行
    private static int level = 2;

    private List<Line> table = new ArrayList<>();

    private Map<Long, Set<Node>> transactionMap = new ConcurrentHashMap<>();

    public long startTransaction() {
        return System.currentTimeMillis() + (long) (Math.random() * 1000);
    }

    public void commit(long transactionId) {
        Set<Node> updateNodes = transactionMap.get(transactionId);
        updateNodes.forEach(node -> {
            node.commit = true;
            node.line.node = node;
            node.line.updateId.set(0);
        });
    }

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
                while (node != null){
                    if(node.isUpdate && node.commit){
                        value = node.value;
                    }
                    if(node.isUpdate && node.transactionId == transactionId){
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

    public boolean update(Object value, int lineNum, long transactionId) {
        if (lineNum >= table.size()) {
            return false;
        }
        Line line = table.get(lineNum);
        long updateId = line.updateId.get();
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
        Set<Node> nodeList = transactionMap.computeIfAbsent(transactionId, k -> new HashSet<>());
        nodeList.add(node);
        return true;
    }

    private Node flushMyNode(Node node, Object value, long transactionId) {
        Node tail = node;
        while (tail.next != null && !(tail.isUpdate || tail.transactionId != transactionId)) {
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
}
