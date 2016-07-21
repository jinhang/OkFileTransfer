package myfastfile;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Target {

    private FastInst inst;
    private long high;
    private long low;
    private String fileName;
    private long position;
    private RandomAccessFile file;
    private RandomAccessFile cfgFile;
    private String cfgFileName;
    private long expiredTime = 600000L;
    private boolean finish;
    private boolean broken;
    private long sleep;

    private long updateDate;
    private int packet;

    private Window receivingWindow;
    private Window confirmingWindow;
    private Window confirmedWindow;

    private Header header;
    private Udp udp;

    private int port;

    public Target(long high, long low, String name) {
        this.header = new Header();
        fileName = FastConfig.FilePath + "[" + UuidUtils.base58Uuid(high, low) + "]" + name;
        cfgFileName = fileName + FastConfig.ConfigFileExtension;
        this.updateDate = System.currentTimeMillis();

        this.sleep = 1000;
        this.udp = new Udp();
    }

    public long getCurrentWindow() {
        return (this.position - this.position % FastConfig.WindowLength) / FastConfig.WindowLength;
    }

    public void open() {
        try {

            file = new RandomAccessFile(fileName, "rw");

            setPosition(0L);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
            file = null;
        }
        Path cfgFilePath = FileSystems.getDefault().getPath(cfgFileName);

        boolean pathExists = Files.exists(cfgFilePath,
                new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
        if (pathExists) {
            try {
                cfgFile = new RandomAccessFile(cfgFileName, "rw");
                setPosition(cfgFile.readLong());

            } catch (FileNotFoundException ex) {
                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                cfgFile = new RandomAccessFile(cfgFileName, "rw");
                cfgFile.writeLong(getPosition());

            } catch (FileNotFoundException ex) {
                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Target.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void update() {

        this.updateDate = System.currentTimeMillis();
        this.setPacket(0);
    }

    public void close() {
        try {
            if (this.file != null) {
                this.file.close();
                this.file = null;
            }

        } catch (IOException ex) {
            Logger.getLogger(Target.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        try {
            if (this.cfgFile != null) {
                this.cfgFile.close();
                this.cfgFile = null;
            }
        } catch (IOException ex) {
            Logger.getLogger(Target.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void write(byte[] buffer) {
        try {
            if (file == null) {
                this.open();
            }
            file.write(buffer);
            this.setPosition(file.getFilePointer());

        } catch (IOException ex) {
            Logger.getLogger(Target.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void write(long windowId, byte[] buffer) {
        long pos = windowId * FastConfig.WindowLength;

        if (file == null) {
            this.open();
        }

        try {
            this.file.seek(pos);
            file.write(buffer);
            this.setPosition(file.getFilePointer());
            this.cfgFile.seek(0);
            this.cfgFile.writeLong(getPosition());

        } catch (IOException ex) {
            Logger.getLogger(Target.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

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
     * @return the packet
     */
    public int getPacket() {
        return packet;
    }

    /**
     * @param packet the packet to set
     */
    public void setPacket(int packet) {
        this.packet = packet;
    }

    /**
     * @return the position
     */
    public long getPosition() {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(long position) {
        this.position = position;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the receivingWindow
     */
    public Window getReceivingWindow() {
        return receivingWindow;
    }

    /**
     * @param receivingWindow the receivingWindow to set
     */
    public void setReceivingWindow(Window receivingWindow) {
        this.receivingWindow = receivingWindow;
    }

    /**
     * @return the confirmingWindow
     */
    public Window getConfirmingWindow() {
        return confirmingWindow;
    }

    /**
     * @param confirmingWindow the confirmingWindow to set
     */
    public void setConfirmingWindow(Window confirmingWindow) {
        this.confirmingWindow = confirmingWindow;
    }

    /**
     * @return the confirmedWindow
     */
    public Window getConfirmedWindow() {
        return confirmedWindow;
    }

    /**
     * @param confirmedWindow the confirmedWindow to set
     */
    public void setConfirmedWindow(Window confirmedWindow) {
        this.confirmedWindow = confirmedWindow;
    }

}
