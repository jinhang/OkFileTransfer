package myfastfile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Udp {

    private DatagramChannel channel;

    private byte[] buffer;
    private Header header;
    private Result result;

    private SocketAddress address;

    public Udp(InetAddress address, int port, int recvPort) {

        this.address = new InetSocketAddress(address, port);
        this.buffer = new byte[FastConfig.DataLength];
        this.header = new Header();
        this.result = new Result();

        try {
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(recvPort));
        } catch (UnknownHostException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Udp(int recvPort) {

        this.buffer = new byte[FastConfig.DataLength];
        this.header = new Header();
        this.result = new Result();

        try {
            channel = DatagramChannel.open();
            channel.socket().setSoTimeout(10000);
            channel.socket().bind(new InetSocketAddress(recvPort));

        } catch (UnknownHostException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public Udp() {

        this.buffer = new byte[FastConfig.DataLength];
        this.header = new Header();
        this.result = new Result();

        try {
            channel = DatagramChannel.open();
            channel.socket().setSoTimeout(10000);

        } catch (UnknownHostException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void send(byte[] message) throws IOException {
        ByteBuffer sendBuf = ByteBuffer.allocate(message.length);
        sendBuf.clear();
        sendBuf.put(message);
        sendBuf.flip();
        channel.send(sendBuf, address);

    }

    public void send(byte[] message, SocketAddress address) throws IOException {
        ByteBuffer sendBuf = ByteBuffer.allocate(message.length);
        sendBuf.clear();
        sendBuf.put(message);
        sendBuf.flip();
        channel.send(sendBuf, address);

    }

    public byte[] recv() {
        try {
            ByteBuffer recvBuf = ByteBuffer.allocate(FastConfig.DataLength);
            recvBuf.clear();
            address = channel.receive(recvBuf);
            return recvBuf.array();
        } catch (Exception ex) {
            Logger.getLogger(Udp.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public void close() throws IOException {
        this.channel.close();
    }

    /**
     * @return the address
     */
    public SocketAddress getAddress() {

        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }

}
