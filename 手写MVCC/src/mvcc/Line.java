package mvcc;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Line {

    public Node node;

    public int lineNum;

    public AtomicLong updateId = new AtomicLong(0);
}
