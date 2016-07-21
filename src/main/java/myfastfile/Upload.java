package myfastfile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Upload implements Runnable {

    private long low;
    private long high;
    private long sleep;
    private String id;
    private Date createDate;
    private Quiver quiver;
    private Udp udp;
    private Header header;

    private volatile boolean finish;

    private volatile boolean broken;

    private volatile long bandwidth = 100000L;

    private volatile long lastMaxBandwidth = 0L;

    private volatile long limitBandwidth = 1000000L;

    private long expiredTime = 600000L;

    private long updateDate;

    private long syncCounter;

    private ConcurrentLinkedQueue<Miss> queue;

    public Upload(Udp udp, Quiver quiver) {
        this.header = new Header();
        this.quiver = quiver;
        this.udp = udp;
        this.syncCounter = 0;
        queue = new ConcurrentLinkedQueue<>();

        this.createDate = new Date(System.currentTimeMillis());
        this.updateDate = System.currentTimeMillis();

    }

    public boolean start(String fileName) {

        this.beginUpload(fileName);
        byte[] bytes = this.udp.recv();
        if (bytes != null) {
            this.header.load(bytes);
            if (this.header.getHigh() == this.getHigh() && this.header.getLow() == this.getLow()) {
                this.quiver.setUploadingWindow(this.header.getWindow());
                this.quiver.setConfirmingWindow(this.header.getWindow()-1);
                return true;
            } else {
                return false;
            }
        }
        while (bytes == null) {
            this.beginUpload(fileName);
            bytes = this.udp.recv();
            if (bytes != null) {
                this.header.load(bytes);
                if (this.header.getHigh() == this.getHigh() && this.header.getLow() == this.getLow()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        //每十秒重试一次，直到获取回包成功

        return false;
    }

    public void setAddress(InetSocketAddress address) {
        this.udp.setAddress(address);
    }

    public void miss(Miss miss) {

        this.queue.add(miss);
    }

    public void cleanMiss() {

        this.queue.clear();
    }

    public void update() {

        this.updateDate = System.currentTimeMillis();

    }

    public long getTransferred() {
        return this.getQuiver().getUploadingWindow() * FastConfig.WindowLength;
    }

    public String getFileName() {
        return this.getQuiver().getFileName();
    }

    public int getPacket(long window) {
        if (this.getQuiver().getUploadingWindow() == window) {
            return this.getQuiver().getPacketId();
        } else {
            if (this.getQuiver().getUploadingWindow() > window) {
                return FastConfig.PacketInWindow;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void run() {

        ///正常射击
        //补射
        while (!this.finish) {
            try {
                Thread.sleep(sleep);
                if (!this.broken) {

                    if ((System.currentTimeMillis() - this.updateDate) < this.expiredTime) {
                        this.broken = false;

                    } else {
                        this.broken = true;
                    }
                    Miss miss = this.queue.poll();
                    if (miss != null) {
                        this.retry(miss);
                    } else {
                        if (this.syncCounter > FastConfig.PacketInWindow / 2) {
                            this.syncCounter = 0;
                            if (this.quiver.getUploadingWindow() > 1) {
                                this.askUploadStatus(this.quiver.getConfirmingWindow());
                            }
                        }
                        if (!this.quiver.isFinish()) {

                            if (this.quiver.hasNext()) {
                                this.upload();
                                if (this.quiver.getUploadingWindow() == 3) {
                                    System.out.print("");
                                }
                            }

                            this.syncCounter++;
                        } else {
                            notifyFinish();
                            Thread.sleep(500L);
                        }

                    }
                }
                sleep = (1000 * FastConfig.DataLength) / getBandwidth();

            } catch (InterruptedException ex) {
                Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            this.getQuiver().close();
            this.udp.close();
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void retry(Miss miss) {
        try {
            byte[] bytes = this.getQuiver().fetch(miss.getWindow(), miss.getPacket());
            header.setId(miss.getPacket());
            header.setWindow(miss.getWindow());
            header.setLength(bytes.length);
            header.setSize(this.getQuiver().getFileSize());
            header.setLow(getLow());
            header.setHigh(getHigh());
            header.setType(PacketType.RetryData.ordinal());
            byte[] headData = header.data();
            byte[] packetData = new byte[headData.length + bytes.length];
            System.arraycopy(headData, 0, packetData, 0, headData.length);
            System.arraycopy(bytes, 0, packetData, headData.length, bytes.length);
            this.udp.send(packetData);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void askUploadStatus(long ask) {
        try {

            header.setId(0);
            header.setWindow(ask);
            header.setLength(0);
            header.setSize(this.quiver.getFileSize());
            header.setLow(this.getLow());
            header.setHigh(this.getHigh());
            header.setScore(0);
            header.setType(PacketType.UploadStatus.ordinal());
            byte[] headData = header.data();
            this.udp.send(headData);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void upload() {
        try {
            header.setId(this.getQuiver().getPacketId());
            header.setWindow(this.getQuiver().getUploadingWindow());
            header.setScore(this.getQuiver().getPacketId());
            byte[] bytes = this.getQuiver().fetch();
            header.setSize(this.getQuiver().getFileSize());
            header.setLength(bytes.length);
            header.setLow(getLow());
            header.setHigh(getHigh());
            header.setType(PacketType.Data.ordinal());

            byte[] headData = header.data();
            byte[] packetData = new byte[headData.length + bytes.length];
            System.arraycopy(headData, 0, packetData, 0, headData.length);
            System.arraycopy(bytes, 0, packetData, headData.length, bytes.length);
            this.udp.send(packetData);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void notifyFinish() {
        try {
            header.setId(0);
            header.setWindow(0);
            header.setScore(0);
            header.setSize(0);
            header.setLength(0);
            header.setLow(this.low);
            header.setHigh(this.high);
            header.setType(PacketType.Finish.ordinal());
            byte[] headData = header.data();
            this.udp.send(headData);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void beginUpload(String fileName) {
        try {
            header.setId(this.getQuiver().getPacketId());
            header.setWindow(this.getQuiver().getUploadingWindow());
            header.setScore(this.getQuiver().getPacketId());
            byte[] bytes = fileName.getBytes();
            header.setSize(this.getQuiver().getFileSize());
            header.setLength(bytes.length);
            header.setLow(getLow());
            header.setHigh(getHigh());
            header.setType(PacketType.BeginUpload.ordinal());

            byte[] headData = header.data();
            byte[] packetData = new byte[headData.length + bytes.length];
            System.arraycopy(headData, 0, packetData, 0, headData.length);
            System.arraycopy(bytes, 0, packetData, headData.length, bytes.length);
            this.udp.send(packetData);
        } catch (IOException ex) {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @return the createDate
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * @return the bandwidth
     */
    public long getBandwidth() {
        if (bandwidth == 0) {
            bandwidth = 10000;
        }
        return bandwidth;
    }

    /**
     * @param bandwidth the bandwidth to set
     */
    public void setBandwidth(long bandwidth) {
        if (bandwidth > getLimitBandwidth()) {
            bandwidth = getLimitBandwidth();
        }
        if (bandwidth > this.getLastMaxBandwidth()) {
            this.lastMaxBandwidth = bandwidth;
        }
        this.bandwidth = bandwidth;
    }

    /**
     * @return the lastMaxBandwidth
     */
    public long getLastMaxBandwidth() {
        return lastMaxBandwidth;
    }

    /**
     * @return the limitBandwidth
     */
    public long getLimitBandwidth() {
        return limitBandwidth;
    }

    /**
     * @param limitBandwidth the limitBandwidth to set
     */
    public void setLimitBandwidth(long limitBandwidth) {
        this.limitBandwidth = limitBandwidth;
    }

    /**
     * @return the low
     */
    public long getLow() {
        return low;
    }

    /**
     * @param low the low to set
     */
    public void setLow(long low) {
        this.low = low;
    }

    /**
     * @return the high
     */
    public long getHigh() {
        return high;
    }

    /**
     * @param high the high to set
     */
    public void setHigh(long high) {
        this.high = high;
    }

    /**
     * @return the udp
     */
    public Udp getUdp() {
        return udp;
    }

    /**
     * @return the quiver
     */
    public Quiver getQuiver() {
        return quiver;
    }

    /**
     * @return the finish
     */
    public boolean isFinish() {
        return finish;
    }

    /**
     * @param finish the finish to set
     */
    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    /**
     * @return the broken
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * @param broken the broken to set
     */
    public void setBroken(boolean broken) {
        this.broken = broken;
    }

}
