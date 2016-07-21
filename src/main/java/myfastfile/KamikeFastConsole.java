package myfastfile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author jinhang
 *
 */
public class KamikeFastConsole {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // TODO code application logic here
            
            InetAddress address = InetAddress.getLocalHost();
            int port = 3800;
            Udp bow = new Udp(address, 3800,35000);
            String name = "d:\\data\\d.mp4";
            Quiver quiver = new Quiver(name);
            quiver.open();
            Upload archer = new Upload(bow, quiver);
            FastInst.getInstance().start(archer,"loveyouyou.mp4");
            System.out.println("sart");
            while (FastInst.getInstance().Listen) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    Logger.getLogger(KamikeFastConsole.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(KamikeFastConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
