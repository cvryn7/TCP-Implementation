import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
/*
 * This class provide TCP Socker server functionality for
 * getting socket for the specific requesting client
 * Author : Karan Bhagat
 * Modified : 8 Nov 2015
 */
public class MyTCPServerSocket {

	int serverPort;//server port number
	DatagramSocket serverSocket;//datagram socket
	InetAddress clientIp;//IP address of the client
	int clientPort;//Port of the client
	int client_isn;//Client sequence number
	int server_isn;//Server sequence number
	
	//constructor
	public MyTCPServerSocket(int port){
		serverPort = port;
	}

	//Methord for initiating handshake and receiving back socket to client
	MyTCPSocket accept(){
		
		//handshake process
		try {
			serverSocket = new DatagramSocket(serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		///listening for message from the client
		listen(1);
		System.out.println("got data 1");
		reply();
		System.out.println("got data 2");
		listen(3);
		System.out.println("finished handshake!");
		serverSocket.close();
		MyTCPSocket clientSocket = new MyTCPSocket(serverPort, clientIp,clientPort,1);
		clientSocket.setHost_isn(server_isn);
		clientSocket.setDest_isn(client_isn);
		return clientSocket;
 
	}
	
	//parameter listen for
	// 1 -- means for syn only
	// 2 -- means for ack only
	// 3 -- means for syn and ack both
	void listen(int handle){
	
		byte[] reqRecvData = new byte[1000];
		DatagramPacket recvPacket;
		ByteArrayInputStream byteRecvStream;
		ObjectInputStream objectRecvStream;
		boolean whileFlag = true;
		
		try {
			recvPacket = new DatagramPacket(reqRecvData,reqRecvData.length);
			//listening until receive some packets
			while(whileFlag){
				serverSocket.receive(recvPacket);
				System.out.println("got handshake");
				byteRecvStream = new ByteArrayInputStream(reqRecvData);
				objectRecvStream = new ObjectInputStream(new BufferedInputStream(byteRecvStream));

				MyTCPPacket newPacket = (MyTCPPacket)objectRecvStream.readObject();
				
				switch(handle){
				case 1:
					if( newPacket.syn ){
						clientPort = recvPacket.getPort();
						clientIp = recvPacket.getAddress();
						client_isn = newPacket.getSeqNum()+1;
						whileFlag = false;
					}
					break;
				case 2:
					if( newPacket.ack ){
					//no functionality needed in this module	
					}
					break;
				case 3:
					if( newPacket.syn && newPacket.ack){
						System.out.println("in handle 3");
						if( newPacket.getSeqNum() == client_isn){
							client_isn += 1;
							whileFlag = false;
						}
					}
					break;
				}
				
			}	
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//Method for sending back handshake reply to the server
	void reply(){
		byte[] sendData;
		DatagramPacket sendPacket;
		ByteArrayOutputStream byteSendStream;
		ObjectOutputStream objectSendStream;
		
		try {
			
			Random rGenerator = new Random();
			server_isn = rGenerator.nextInt(10000);
			System.out.println("initial server isn : "+server_isn);
			//my tcp packet with seqNum, synbit, ackbit, ackNum
			MyTCPPacket handShakePacket = new MyTCPPacket(server_isn,true,true,client_isn+1);
			
			byteSendStream = new ByteArrayOutputStream(1000);
			objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
			objectSendStream.flush();
			objectSendStream.writeObject(handShakePacket);
			objectSendStream.flush();
			sendData = byteSendStream.toByteArray();
			sendPacket = new DatagramPacket(sendData,sendData.length,clientIp,clientPort);
			serverSocket.send(sendPacket);
			objectSendStream.close();
			byteSendStream.close();
			server_isn++;
			
			
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
