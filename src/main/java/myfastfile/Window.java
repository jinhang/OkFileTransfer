package myfastfile;


public class Window {

    private long id;
    private long low;
    private long high;
    private byte[] buffer;

    private byte[] hits;

    private boolean full;
    private int hitCount;

    private boolean isLastWindow;

    public boolean isLastWindow() {
        return isLastWindow;
    }

    public Window(Header header) {

        this.id = header.getWindow();
        this.high = header.getHigh();
        this.low = header.getLow();
        long size = header.getSize();

        long lastWindow = (size - size % FastConfig.WindowLength) / FastConfig.WindowLength;
        int lastWindowLength = (int) (size % FastConfig.WindowLength);
        int lastPacket = (int) (lastWindowLength - lastWindowLength % FastConfig.PacketLength) / FastConfig.PacketLength;

        if (header.getWindow() < lastWindow) {
            this.buffer = new byte[FastConfig.WindowLength];
            this.hits = new byte[FastConfig.PacketInWindow];
            this.isLastWindow = false;
        } else {
            this.buffer = new byte[lastWindowLength];
            this.hits = new byte[lastPacket + 1];
            this.isLastWindow = true;
        }

        this.full = false;
        this.hitCount = 0;

    }

    @Override
    public boolean equals(Object o) {
        if (this.hashCode() == o.hashCode()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (int) (this.id ^ (this.id >>> 32));
        hash = 71 * hash + (int) (this.low ^ (this.low >>> 32));
        hash = 71 * hash + (int) (this.high ^ (this.high >>> 32));
        return hash;
    }

    public boolean isFull() {
        if (full) {
            return full;
        }
        if (hits == null) {
            return false;
        }
        for (byte hit : hits) {
            if (hit == 0x0) {
                this.full = false;
                return full;
            }
        }
        this.full = true;
        return full;
    }

    public void setData(int position, byte[] data) {

        if (hits == null) {
            hits = new byte[FastConfig.PacketInWindow];
            setHitCount(0);
        }
        //已经写过的旧丢弃
        if (hits[position] == 0x0) {
            hits[position] = 0x1;
            System.arraycopy(data, 0, buffer, position * FastConfig.PacketLength, data.length);
            this.hitCount++;
        }

    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
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
     * @return the hits
     */
    public byte[] getHits() {
        return hits;
    }

    /**
     * @param hits the hits to set
     */
    public void setHits(byte[] hits) {
        this.hits = hits;
    }

    /**
     * @return the hitCount
     */
    public int getHitCount() {
        return hitCount;
    }

    /**
     * @param hitCount the hitCount to set
     */
    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

}
