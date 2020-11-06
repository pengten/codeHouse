package deadlock;

public class Main {
    public static void main(String[] args) {
        Deadlock<Long,Integer> deadlock = new Deadlock<>();
        new Thread(()->{
            Deadlock.Status status;
            while ((status = deadlock.tryLock(1l,1)) != Deadlock.Status.OK){
                if(status == Deadlock.Status.DEAD){
                    System.out.println("1:������");
                    return;
                }
                if(status == Deadlock.Status.WAITED){
                    System.out.println("1:����ͬʱ���������Դ");
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("1:��ȡ����Դ1");
            while ((status = deadlock.tryLock(1l,2)) != Deadlock.Status.OK){
                if(status == Deadlock.Status.DEAD){
                    System.out.println("1:������");
                    return;
                }
                if(status == Deadlock.Status.WAITED){
                    System.out.println("1:����ͬʱ���������Դ");
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("1:��ȡ����Դ2");
            deadlock.release(1l,1);
            deadlock.release(1l,2);
        }).start();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Deadlock.Status status;
        while ((status = deadlock.tryLock(2l,1)) != Deadlock.Status.OK){
            if(status == Deadlock.Status.DEAD){
                System.out.println("2:������");
                return;
            }
            if(status == Deadlock.Status.WAITED){
                System.out.println("2:����ͬʱ���������Դ");
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("2:��ȡ����Դ1");
        while ((status = deadlock.tryLock(2l,2)) != Deadlock.Status.OK){
            if(status == Deadlock.Status.DEAD){
                System.out.println("2:������");
                return;
            }
            if(status == Deadlock.Status.WAITED){
                System.out.println("2:����ͬʱ���������Դ");
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("2:��ȡ����Դ2");
        deadlock.release(2l,1);
        deadlock.release(2l,2);
    }
}
