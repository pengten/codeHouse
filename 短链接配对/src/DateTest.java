import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DateTest {

    private static Map<String, String> recordMap = new ConcurrentHashMap<>();

    private static Map<String, String> heartMap = new ConcurrentHashMap<>();

    private static BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws InterruptedException {
//        Thread thread1 = start("111", "111配对成功：");
//        Thread thread2 = start("222", "222配对成功：");
//        Thread thread3 = start("333", "333配对成功：");
//        Thread thread4 = start("444", "444配对成功：");
//        thread1.start();
//        Thread.sleep(100);
//        thread2.start();
//        thread3.start();
//        thread4.start();
        long start = System.currentTimeMillis();
        Thread thread1 = start("1", "1s配对成功：");
        thread1.start();
        for (int i = 0; i < 1000; i++) {
            Thread thread = start(i+"", i+"配对成功：");
            thread.start();
        }
        System.out.println("all run time："+ (System.currentTimeMillis()-start));
    }

    private static Thread start(String s, String s2) {
        return new Thread(() -> {
            String record;
            while (true) {
                try {
                    if ((record = record(s)) != null) break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(s2 + record);
        });
    }

    private static String record(String id) throws InterruptedException {
        String record = recordMap.get(id);
        if (record == null || record.startsWith("success")) {
            record = recordMap.get(id);
            if (record == null || record.startsWith("success")) {
                record = "wait,";
                recordMap.put(id, record);
                queue.put(id);
            }
        }
        String[] split = record.split(",");
        if (split[0].equals("process")) {
            refreshHeart("process",id);
            return record;
        }
        String targetId = null;
        for (int i = 0; i < 10; i++, Thread.sleep(100)) {
            targetId = queue.poll(1, TimeUnit.SECONDS);
            if (targetId == null) {
                break;
            }
            if (targetId.equals(id)) {
                System.out.println("匹配到自己:" + id);
                queue.put(id);
                targetId = null;
                continue;
            }
            System.out.println("找到对方:" + id+","+targetId);
            break;
        }
        if (targetId == null) {
            System.out.println("没有人配对:" + id);
            return null;
        }
        String targetRecord = recordMap.get(targetId);
        if(targetRecord.startsWith("process")){
            System.out.println("对方已经配对:" + id+","+targetId);
            return null;
        }
        if (changeStatus(id, targetId)) {
            return recordMap.get(id);
        } else {
            queue.put(targetId);
            System.out.println("修改状态失败:" + id);
            return null;
        }
    }

    private static boolean changeStatus(String id, String targetId) {
        long start = System.currentTimeMillis();
        synchronized (recordMap) {
            if (!(recordMap.get(id).startsWith("wait") && recordMap.get(targetId).startsWith("wait"))) {
                return false;
            }
            recordMap.computeIfPresent(id, (k, v) -> v.startsWith("wait") ? "process," + targetId : v);
            recordMap.computeIfPresent(targetId, (k, v) -> v.startsWith("wait") ? "process," + id : v);
        }
        return true;
    }

    private static boolean refreshHeart(String status, String id) {
        synchronized (heartMap) {
            String heart = heartMap.get(id);
            if(heart == null){
                heart = status+","+System.currentTimeMillis();
            }else {
                heart.replaceAll(",.*", "," + System.currentTimeMillis());
            }
            heartMap.put(id, heart);
        }
        return true;
    }
}