package myfastfile;

import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Reporter implements Runnable {

    DatagramSocket socket;

    Upload archer;
    byte[] buffer;

    private Header header;
    private Result result;
    private double lost;

    public Reporter(Upload archer) {
        this.archer = archer;

        this.buffer = new byte[FastConfig.DataLength];
        this.header = new Header();
        this.result = new Result();

    }

    @Override
    public void run() {
        while (!archer.isFinish()) {

            try {

                byte[] packetBytes = this.archer.getUdp().recv();
                //瑙ｆ瀽澶撮儴
                this.header.load(packetBytes);

                //鏄惁鏄暟鎹殑
                PacketType type = PacketType.values()[this.header.getType()];

                switch (type) {
                    //涓轰簡鍙戦�侊紝鍙戠幇涓㈠寘锛岃皟鏁撮�熷害
                    case UploadStatus:
                        archer.update();//鏈夊洖鍖咃紝璇存槑缃戠粶OK

                        if (header.getScore() >= FastConfig.PacketInWindow
                                || (header.getWindow() == this.archer.getQuiver().getLastWindow()
                                && header.getScore() >= this.archer.getQuiver().getLastPacket() + 1)) {
                            this.archer.getQuiver().setConfirmedWindow(header.getWindow());

                            continue;
                        }

                        int packetSend = archer.getPacket(header.getWindow());
                        lost = (double) (packetSend - header.getScore()) / packetSend;
                        //archer.setBandwidth((long)(archer.getBandwidth()/lost));
                        if (lost > 0.10 && lost < 1.0) {
                            archer.setBandwidth((long) (archer.getBandwidth() * (1 - lost)));
                        } else {
                            if (lost < 0.01) {
                                archer.setBandwidth((long) (archer.getBandwidth() * 1.2));
                            } else {
                                //鍟ヤ篃涓嶅仛淇濇寔鍘熼��
                            }
                        }
                        //琛ュ皠

                        this.result.load(header, packetBytes);
                        //archer.cleanMiss();
                        for (int i = 0; i < packetSend; i++) {
                            byte[] data = result.getBuffer();
                            if (data[i] == 0x0) {
                                Miss miss = new Miss();
                                miss.setPacket(i);
                                miss.setWindow(header.getWindow());
                                archer.miss(miss);
                            }
                        }

                        break;
                    case Finish:

                        archer.update();//鏈夊洖鍖咃紝璇存槑缃戠粶OK
                        this.archer.setFinish(true);

                        break;

                }
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                Logger.getLogger(Reporter.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}
