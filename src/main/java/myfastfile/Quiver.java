package myfastfile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Quiver {

    public Quiver(String fileName) {
        this.fileName = fileName;
        this.offset = 0;
        this.byteData = new byte[FastConfig.PacketLength];
        this.confirmedWindow=-1;

    }
    private ConcurrentHashMap<Long, byte[]> hashMap = new ConcurrentHashMap<>();

    public boolean open() {
        try {
            setFile(new RandomAccessFile(getFileName(), "r"));
            this.setFileSize(this.file.length());
            this.lastWindow = (this.getFileSize() - this.getFileSize() % FastConfig.WindowLength) / FastConfig.WindowLength;
            long lastWindowLength = (this.getFileSize() % FastConfig.WindowLength);
            this.lastPacket = (lastWindowLength - lastWindowLength % FastConfig.PacketLength) / FastConfig.PacketLength;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Quiver.class.getName()).log(Level.SEVERE, null, ex);
            setFile(null);
            return false;
        } catch (IOException ex) {
            Logger.getLogger(Quiver.class.getName()).log(Level.SEVERE, null, ex);
            setFile(null);
            return false;
        }
        return true;
    }
    private String fileName;

    private long fileSize;
    private RandomAccessFile file;
    private long offset;
    private byte[] buffer;

    private long lastWindow;
    private long lastPacket;

    private byte[] byteData;

    private volatile boolean finish;
    

    private long uploadingWindow;
    private int packetId;
    private long confirmingWindow;
    private long confirmedWindow;

    public boolean hasNext() {
         
        if(uploadingWindow<2)
            return true;
        if (uploadingWindow-confirmedWindow <= 3) {
            return true;
        } else {
            return false;
        }
    }

     

    public byte[] fetch() {
        this.finish = false;
       
        byte[] result = fetch(this.uploadingWindow, this.packetId);
        if (this.uploadingWindow == lastWindow && this.packetId == this.lastPacket) {
            this.finish = true;
            //宸茬粡鍒版渶鍚庝簡
            return result;
        }
        if (this.packetId < FastConfig.PacketInWindow - 1) {
            this.packetId++;
            //涓嶆槸鏈�鍚庝竴涓姤鏂�
            return result;
        }
        //褰搘indow涓嶆槸鏈�鍚庝竴涓獁indow锛岃�屼笖this.packetId鏄渶鍚庝竴涓紝閭ｄ箞鏇存崲window
        if (this.uploadingWindow < lastWindow && this.packetId == FastConfig.PacketInWindow - 1) {
            this.confirmingWindow = this.uploadingWindow;
            this.uploadingWindow = this.confirmingWindow + 1;
           
            this.packetId = 0;
           
        }

        return result;

    }

    public byte[] fetch(long windowId, int packetId) {

        try {
            int packetOffset = packetId * FastConfig.PacketLength;

            if (this.file == null) {
                return null;
            }

            if (windowId > lastWindow || windowId < 0 || packetId < 0 || packetId > FastConfig.PacketInWindow) {
                return null;
            }
            if (windowId == lastWindow && packetId > this.lastPacket) {
                return null;
            }

            setOffset(windowId * FastConfig.WindowLength);
            setBuffer(new byte[FastConfig.WindowLength]);

            if (this.offset + FastConfig.WindowLength > this.getFileSize()) {

                int lastLength = (int) (this.getFileSize() - this.offset);
                setBuffer(new byte[lastLength]);

            }
            if (this.hashMap.containsKey(windowId)) {
                setBuffer(this.hashMap.get(windowId));
            } else {
                this.file.seek(this.offset);
                this.file.read(getBuffer(), 0, getBuffer().length);
                this.hashMap.put(windowId, getBuffer());
                if (this.hashMap.containsKey(windowId - 2L)) {
                    this.hashMap.remove(windowId - 2L);
                }
            }
            if (packetOffset + FastConfig.PacketLength > getBuffer().length) {
                setByteData(new byte[  getBuffer().length-packetOffset]);
                //鍑鸿礋鏁颁簡

            } else {
                setByteData(new byte[this.byteData.length]);
            }
            System.arraycopy(getBuffer(), packetOffset, this.byteData, 0, this.byteData.length);

        } catch (IOException ex) {
            Logger.getLogger(Quiver.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return this.byteData;

    }

    public void close() {
        try {
            this.file.close();
            this.hashMap.clear();
        } catch (Exception ex) {
            Logger.getLogger(Quiver.class.getName()).log(Level.SEVERE, null, ex);
            this.hashMap = null;
            this.setFile(null);
        }
    }

    /**
     * @return the hashMap
     */
    public ConcurrentHashMap<Long, byte[]> getHashMap() {
        return hashMap;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the fileSize
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * @param fileSize the fileSize to set
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the file
     */
    public RandomAccessFile getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(RandomAccessFile file) {
        this.file = file;
    }

    /**
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
     * @return the buffer
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * @param buffer the buffer to set
     */
    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    /**
     * @return the lastWindow
     */
    public long getLastWindow() {
        return lastWindow;
    }

    /**
     * @param lastWindow the lastWindow to set
     */
    public void setLastWindow(long lastWindow) {
        this.lastWindow = lastWindow;
    }

    /**
     * @return the byteData
     */
    public byte[] getByteDate() {
        return byteData;
    }

    /**
     * @param byteData the byteData to set
     */
    public void setByteData(byte[] byteData) {
        this.byteData = byteData;
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
     * @return the uploadingWindow
     */
    public long getUploadingWindow() {
        return uploadingWindow;
    }

    /**
     * @param uploadingWindow the uploadingWindow to set
     */
    public void setUploadingWindow(long uploadingWindow) {
        this.uploadingWindow = uploadingWindow;
    }

    /**
     * @return the packetId
     */
    public int getPacketId() {
        return packetId;
    }

    /**
     * @param packetId the packetId to set
     */
    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    /**
     * @return the confirmingWindow
     */
    public long getConfirmingWindow() {
        return confirmingWindow;
    }

    /**
     * @param confirmingWindow the confirmingWindow to set
     */
    public void setConfirmingWindow(long confirmingWindow) {
        this.confirmingWindow = confirmingWindow;
    }

    /**
     * @return the confirmedWindow
     */
    public long getConfirmedWindow() {
        return confirmedWindow;
    }

    /**
     * @param confirmedWindow the confirmedWindow to set
     */
    public void setConfirmedWindow(long confirmedWindow) {
        this.confirmedWindow = confirmedWindow;
    }

    /**
     * @return the lastPacket
     */
    public long getLastPacket() {
        return lastPacket;
    }

    /**
     * @param lastPacket the lastPacket to set
     */
    public void setLastPacket(long lastPacket) {
        this.lastPacket = lastPacket;
    }

    
}
