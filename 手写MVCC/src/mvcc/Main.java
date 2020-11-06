package mvcc;

public class Main {
    public static void main(String[] args) {
        MVCC mvcc = new MVCC();
        long id = mvcc.startTransaction();
        long id2 = mvcc.startTransaction();
        System.out.println("开启事务1与事务2");
        int lineNum = mvcc.insert(1, id);
        System.out.println("插入数据：1");

        System.out.println("事务1读取数据：" + mvcc.read(lineNum, id));
        System.out.println("事务2读取数据：" + mvcc.read(lineNum, id2));

        mvcc.update(2, lineNum, id);

        System.out.println("事务1修改数据为：2");

        System.out.println("事务1读取数据：" + mvcc.read(lineNum, id));
        System.out.println("事务2读取数据：" + mvcc.read(lineNum, id2));

        mvcc.commit(id);
        System.out.println("事务1提交");
        System.out.println("事务1读取数据：" + mvcc.read(lineNum, id));
        System.out.println("事务2读取数据：" + mvcc.read(lineNum, id2));

        mvcc.update(3, lineNum, id2);
        System.out.println("事务2修改数据为：3");

        System.out.println("事务1读取数据：" + mvcc.read(lineNum, id));
        System.out.println("事务2读取数据：" + mvcc.read(lineNum, id2));

        mvcc.callback(id2);
        System.out.println("事务2回滚");
        System.out.println("事务1读取数据：" + mvcc.read(lineNum, id));
        System.out.println("事务2读取数据：" + mvcc.read(lineNum, id2));
    }

}
