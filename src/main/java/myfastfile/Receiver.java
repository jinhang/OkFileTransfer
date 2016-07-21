package myfastfile;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Receiver implements Runnable {

    private Udp udp;
    int port;
    FastInst inst;
    byte[] buffer;

    private Header header;
    private Result result;
    private double lost;
    private SocketAddress address;

    public Receiver(FastInst inst, int port) {
        this.inst = inst;
        this.port = port;
        this.buffer = new byte[FastConfig.DataLength];
        this.header = new Header();
        this.result = new Result();
        this.udp = new Udp(port);
    }

    //鎺ユ敹鍒颁笅杞戒俊鍙凤紝寮�濮嬩紶杈撴暟鎹�
    public void beginDownload(byte[] data) {
        Upload upload = inst.getUpload(header.getHigh(), header.getLow());
        this.result.load(header, data);
        if (upload == null) {
            String fileName = new String(this.result.getBuffer(), 0, this.header.getLength());
            Quiver quiver = new Quiver(fileName);
            boolean ret = quiver.open();
            if (!ret) {
                return;
            }
            Udp newUdp = new Udp();
            newUdp.setAddress(address);
            upload = new Upload(newUdp, quiver);
            inst.start(upload,fileName);

        }
        upload.update();

    }

    public void report(Window window) {
        try {
            byte[] bytes = window.getHits();
            header.setId(0);
            header.setWindow(window.getId());
            header.setLength(bytes.length);
            header.setSize(0);
            header.setLow(window.getLow());
            header.setHigh(window.getHigh());
            header.setScore(window.getHitCount());
            header.setType(PacketType.UploadStatus.ordinal());
            byte[] headData = header.data();
            byte[] packetData = new byte[headData.length + bytes.length];
            System.arraycopy(headData, 0, packetData, 0, headData.length);
            System.arraycopy(bytes, 0, packetData, headData.length, bytes.length);
            this.udp.send(packetData, address);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void reportFinish() {
        try {

            header.setId(0);
            header.setWindow(0);
            header.setLength(0);
            header.setSize(0);
            header.setLow(header.getLow());
            header.setHigh(header.getHigh());
            header.setScore(0);
            header.setType(PacketType.Finish.ordinal());
            byte[] headData = header.data();

            this.udp.send(headData, address);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //鎺ユ敹鍒颁笂浼犱俊鍙凤紝寮�濮嬪噯澶囨帴鏀舵暟鎹紝閫氱煡杩滄柟寮�濮嬩笂浼狅紝鍙戦�佷笂浼犱俊鍙�
    public void beginUpload(byte[] data) {
        Target target = inst.getTarget(header.getHigh(), header.getLow());
        this.result.load(header, data);
        if (target == null) {
            String fileName = new String(this.result.getBuffer(), 0, header.getLength());
            target = new Target(header.getHigh(), header.getLow(), fileName);
            target.open();
            inst.addTarget(target);
            //涓嬮潰閫氱煡寮�濮嬩笂浼�,缁欒繙鏂瑰彂閫佷笂浼犱俊鍙�
            this.notifyStartUpload(target.getCurrentWindow());
        }
        target.update();

    }

    public void notifyStartUpload(long window) {
        try {

            header.setId(0);
            header.setWindow(window);
            header.setLength(0);
            header.setSize(0);
            header.setLow(header.getLow());
            header.setHigh(header.getHigh());
            header.setScore(0);
            header.setType(PacketType.BeginUpload.ordinal());
            byte[] headData = header.data();
            this.udp.send(headData, address);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //鎺ユ敹鏁版嵁
    public void receiving(byte[] data) {
        Target target = inst.getTarget(header.getHigh(), header.getLow());
        if (target == null) {
            return;
        }
        if (target.getReceivingWindow() == null) {
            Window win = new Window(header);
            this.result.load(header, data);
            win.setData(header.getId(), this.result.getBuffer());
            target.setReceivingWindow(win);
        }
        if (target.getReceivingWindow().getId() <= header.getWindow()) {
            this.result.load(header, data);
            target.getReceivingWindow().setData(header.getId(), this.result.getBuffer());
            if (target.getReceivingWindow().isFull()) {

                target.write(target.getReceivingWindow().getId(), target.getReceivingWindow().getBuffer());

                target.setConfirmingWindow(target.getReceivingWindow());

                if (target.getReceivingWindow().getId() == header.getLastWindow()) {

                } else {
                    target.setReceivingWindow(null);
                }
                this.report(target.getConfirmingWindow());
            }
        } else {

            target.setConfirmingWindow(target.getReceivingWindow());
            target.setReceivingWindow(null);
        }

    }

    public void retryData(byte[] data) {
        Target target = inst.getTarget(header.getHigh(), header.getLow());
        if (target == null) {
            return;
        }
        if (target.getConfirmingWindow() == null) {
            Window win = new Window(header);
            this.result.load(header, data);
            win.setData(header.getId(), this.result.getBuffer());
            target.setConfirmingWindow(win);
        }
        if (target.getReceivingWindow().getId() == header.getLastWindow()) {
            if (target.getReceivingWindow().getId() == header.getWindow()) {
                this.result.load(header, data);
                target.getReceivingWindow().setData(header.getId(), this.result.getBuffer());
                if (target.getReceivingWindow().isFull()) {
                    //姝ｅソ鏄笅涓�涓獥鍙ｇ殑鎶ユ枃
                    target.write(target.getReceivingWindow().getId(), target.getReceivingWindow().getBuffer());
                    //鍒犻櫎姝ょ獥鍙�
                    this.report(target.getReceivingWindow());
                }
            }
        }
        if (target.getConfirmingWindow().getId() == header.getWindow()) {
            this.result.load(header, data);
            target.getConfirmingWindow().setData(header.getId(), this.result.getBuffer());
            if (target.getConfirmingWindow().isFull()) {
                //姝ｅソ鏄笅涓�涓獥鍙ｇ殑鎶ユ枃
                target.write(target.getConfirmingWindow().getId(), target.getConfirmingWindow().getBuffer());
                //鍒犻櫎姝ょ獥鍙�
                this.report(target.getConfirmingWindow());
                target.setConfirmingWindow(null);
            }
        }

    }

    public void confirm() {
        Target target = inst.getTarget(header.getHigh(), header.getLow());
        if (target == null) {
            return;
        }
        if (target.getConfirmingWindow() == null) {
            //鏄换鍔″惎鍔ㄩ樁娈�
            Window win = new Window(header);
            target.setConfirmingWindow(win);
            if (target.getCurrentWindow() > 0) {
                //鏄柇鐐圭画浼�
                win.setHitCount(FastConfig.PacketInWindow);

            }
        }

        this.report(target.getConfirmingWindow());
    }

    public void finish() {
        Target target = inst.getTarget(header.getHigh(), header.getLow());
        if (target == null) {
            return;
        }
        if (target.getConfirmingWindow() == null) {
            Window win = new Window(header);
            target.setConfirmingWindow(win);
        }
        if (target.getConfirmingWindow().isFull()) {
            if (target.getReceivingWindow().isFull()) {
                this.reportFinish();
                target.close();
            } else {
                this.report(target.getReceivingWindow());
            }
        } else {
            this.report(target.getConfirmingWindow());
        }

    }

    @Override
    public void run() {
        while (inst.Listen) {
            byte[] packet = udp.recv();
            address = udp.getAddress();
            this.header.load(packet);
            PacketType type = PacketType.values()[this.header.getType()];
            switch (type) {
                case UploadStatus:
                    this.confirm();
                    break;
                case Data:
                    this.receiving(packet);
                    break;
                case RetryData:
                    this.retryData(packet);
                    break;
                case BeginUpload:
                    this.beginUpload(packet);
                    break;
                case BeginDownload:
                    this.beginDownload(packet);
                    break;
                case Finish:
                    this.finish();
                    break;

            }
        }
    }

}
