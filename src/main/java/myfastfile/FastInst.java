package myfastfile;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FastInst {

    private static FastInst inst = new FastInst();
    private List<Upload> uploadList;
    private List<Target> targets;
    public volatile boolean Listen = false;
    Thread receiver;

    private FastInst() {
        uploadList = Collections.synchronizedList(new ArrayList<Upload>());
        targets = Collections.synchronizedList(new ArrayList<Target>());
        this.Listen = true;
        receiver = new Thread(new Receiver(this, 3000));
        receiver.setDaemon(true);
        receiver.start();

    }

    public void start(Upload upload,String fileName) {
        if (upload.start(fileName)) {
            Thread shootThread = new Thread(upload);
            shootThread.setDaemon(true);
            shootThread.start();
            Reporter reporter = new Reporter(upload);
            Thread reportThread = new Thread(reporter);
            reportThread.setDaemon(true);
            reportThread.start();
            this.uploadList.add(upload);
        }
    }

    public void refresh() {
        for (Upload upload : uploadList) {
            upload.update();
        }
    }

    public Upload getUpload(long high, long low) {
        for (Upload upload : uploadList) {
            if (upload.getHigh() == high && upload.getLow() == low) {
                return upload;
            }
        }
        return null;
    }

    public Target getTarget(long high, long low) {
        for (Target target : targets) {
            if (target.getHigh() == high && target.getLow() == low) {
                return target;
            }
        }

        return null;
    }

    public void addTarget(Target target) {
        for (Target t : targets) {
            if (t.getHigh() == target.getHigh() && t.getLow() == target.getLow()) {
                return;
            }
        }
        targets.add(target);

    }

    public static FastInst getInstance() {
        return inst;
    }

}
