package myfastfile;
import java.io.BufferedOutputStream; 
import java.io.DataOutputStream; 
import java.io.FileOutputStream; 
import java.io.IOException; 
import java.net.DatagramPacket; 
import java.net.DatagramSocket; 

public class UdpReceive { 

	public static DatagramSocket dataSocket; 
	public static final int PORT =3500; 
	public static byte[] receiveByte; 
	public static DatagramPacket dataPacket; 
	public static void main(String[] args) throws IOException { 
		//3500,35000
		dataSocket = new DatagramSocket(PORT); 
		DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("D:\\data\\c.mp4"))); 
		int i = 0; 
		System.out.println("start");
		while(i == 0){//�����ݣ���ѭ�� 
			receiveByte = new byte[1024]; 
			System.out.println(receiveByte);
			dataPacket = new DatagramPacket(receiveByte, receiveByte.length); 
			dataSocket.receive(dataPacket); 
			i = dataPacket.getLength(); 
			//�������� 
			if(i > 0){ 
				System.out.println("receive");
				//ָ�����յ����ݵĳ��ȣ���ʹ��������������ʾ����ʼʱ�����׺�����һ�� 
				fileOut.write(receiveByte,0,i); 
				fileOut.flush(); 
				i = 0;//ѭ������ 
			} 
		} 
	} 

} 