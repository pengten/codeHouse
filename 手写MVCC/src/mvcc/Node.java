package mvcc;

public class Node {

    public long transactionId;

    public Node pre;

    public Node next;

    public Object value;

    public boolean commit;

    public boolean isUpdate;

    public Line line;

    public Node(long transactionId, Node pre, Node next, Object value, boolean commit) {
        this.transactionId = transactionId;
        this.pre = pre;
        this.next = next;
        this.value = value;
        this.commit = commit;
    }
}
