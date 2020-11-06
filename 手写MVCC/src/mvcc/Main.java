package mvcc;

public class Main {
    public static void main(String[] args) {
        MVCC mvcc = new MVCC();
        long id = mvcc.startTransaction();
        long id2 = mvcc.startTransaction();
        System.out.println("��������1������2");
        int lineNum = mvcc.insert(1, id);
        System.out.println("�������ݣ�1");

        System.out.println("����1��ȡ���ݣ�" + mvcc.read(lineNum, id));
        System.out.println("����2��ȡ���ݣ�" + mvcc.read(lineNum, id2));

        mvcc.update(2, lineNum, id);

        System.out.println("����1�޸�����Ϊ��2");

        System.out.println("����1��ȡ���ݣ�" + mvcc.read(lineNum, id));
        System.out.println("����2��ȡ���ݣ�" + mvcc.read(lineNum, id2));

        mvcc.commit(id);
        System.out.println("����1�ύ");
        System.out.println("����1��ȡ���ݣ�" + mvcc.read(lineNum, id));
        System.out.println("����2��ȡ���ݣ�" + mvcc.read(lineNum, id2));

        mvcc.update(3, lineNum, id2);
        System.out.println("����2�޸�����Ϊ��3");

        System.out.println("����1��ȡ���ݣ�" + mvcc.read(lineNum, id));
        System.out.println("����2��ȡ���ݣ�" + mvcc.read(lineNum, id2));

        mvcc.callback(id2);
        System.out.println("����2�ع�");
        System.out.println("����1��ȡ���ݣ�" + mvcc.read(lineNum, id));
        System.out.println("����2��ȡ���ݣ�" + mvcc.read(lineNum, id2));
    }

}
