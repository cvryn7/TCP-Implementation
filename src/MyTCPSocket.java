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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

/*
 * This class provide TCP socket like functionality
 * 
 * Author : Karan Bhagat
 * Modified : 8 Nov 2015
 */
public class MyTCPSocket {

	
	InetAddress hostAddrs;//host IP address
	int hostPort;//host Port number
	InetAddress destAddress;//destination IP addresss
	int destPort;//destination Port
	DatagramSocket socket;
	int host_isn;//host sequence number
	int dest_isn;//destination sequence number
	int segmentSize = 10000;// in bytes MSS
	int base;// base of the window 
	int sendSeqNum;// next seq number to send
	int windowSize = 1;//size of the window
	LinkedList<MyTCPPacket> recvBuffer = new LinkedList<MyTCPPacket>();//recvBuffer as queue
	volatile boolean keepOnProcessing;

	//constructor
	MyTCPSocket(int port, InetAddress destIp, int destPort){
		hostPort = port;
		destAddress = destIp;
		this.destPort = destPort; 
		try {
			socket = new DatagramSocket(hostPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}

		//intiating handshake
		System.out.println("started handshake");
		intiateHandshake();

	}

	//constructor
	MyTCPSocket(int port, InetAddress destIp, int destPort, int nohandshake){
		hostPort = port;
		destAddress = destIp;
		this.destPort = destPort; 
		try {
			socket = new DatagramSocket(hostPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	//constructor
	MyTCPSocket(int port){
		hostPort = port;
		try {
			socket = new DatagramSocket(hostPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	//Method for implementing handshake
	public void intiateHandshake(){
		System.out.println("send data 1");
		reply(1);
		listenHandshake(1);
		System.out.println("send data 2");
		reply(2);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	//for setting the host sequence number
	public void setHost_isn(int isn){
		host_isn = isn;
	}

	//for setting the destination sequence number
	public void setDest_isn(int isn){
		dest_isn = isn;
	}


	//for intial handshake
	// handle codes
	// 1  means first time send 
	// 2 means replying frist time to server
	public void reply(int handle){
		byte[] sendData;
		DatagramPacket sendPacket;
		ByteArrayOutputStream byteSendStream;
		ObjectOutputStream objectSendStream;
		MyTCPPacket handShakePacket;
		try {
			switch(handle){
			//Replying for the first time
			case 1:
				Random rGenerator = new Random();
				host_isn = rGenerator.nextInt(10000);
				System.out.println("initial client isn : "+host_isn);
				//my tcp packet with seqNum, synbit
				handShakePacket = new MyTCPPacket(host_isn,true);
				byteSendStream = new ByteArrayOutputStream(1000);
				objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
				objectSendStream.flush();
				objectSendStream.writeObject(handShakePacket);
				objectSendStream.flush();
				sendData = byteSendStream.toByteArray();
				sendPacket = new DatagramPacket(sendData,sendData.length,destAddress,destPort);
				//sending packet
				socket.send(sendPacket);
				objectSendStream.close();
				byteSendStream.close();
				host_isn++;
				break;
			//Reply for second time 
			case 2:
				handShakePacket = new MyTCPPacket(host_isn,true,true,dest_isn+1);
				byteSendStream = new ByteArrayOutputStream(1000);
				objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
				objectSendStream.flush();
				objectSendStream.writeObject(handShakePacket);
				objectSendStream.flush();
				sendData = byteSendStream.toByteArray();
				sendPacket = new DatagramPacket(sendData,sendData.length,destAddress,destPort);
				socket.send(sendPacket);
				objectSendStream.close();
				byteSendStream.close();
				host_isn++;
				break;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Method for listening to the handshake packets
	public void listenHandshake(int handle){
		byte[] reqRecvData = new byte[1000];
		DatagramPacket recvPacket;
		ByteArrayInputStream byteRecvStream;
		ObjectInputStream objectRecvStream;
		boolean whileFlag = true;

		try {
			recvPacket = new DatagramPacket(reqRecvData,reqRecvData.length);

			while(whileFlag){
				socket.receive(recvPacket);

				byteRecvStream = new ByteArrayInputStream(reqRecvData);
				objectRecvStream = new ObjectInputStream(new BufferedInputStream(byteRecvStream));

				MyTCPPacket newPacket = (MyTCPPacket)objectRecvStream.readObject();

				switch(handle){
				case 1:
					//check if it is syn bit packet
					if( newPacket.syn ){
						dest_isn = newPacket.getSeqNum()+1;
						whileFlag = false;
					}
					break;
				case 2:
					if( newPacket.ack ){
						//no functionality needed in this module	
					}
					break;
				case 3:
					//check if it a syn and ack packet
					if( newPacket.syn && newPacket.ack){
						if( newPacket.getSeqNum() == dest_isn + 1){
							dest_isn += 1;
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

	//Method for sending the packet
	public void send(MyTCPPacket dataPacket){

		//Start listen acknowledgment thread for this send packet
		ListenAcks hearAck = new ListenAcks();
		Thread ackThread = new Thread(hearAck);
		ackThread.start();

		//intializing all parameter for controlling flow of packets 
		//and window size
		base = host_isn;
		windowSize = 1;
		int nextSeq = base;
		int startSeqNum = base;
		int dataPointer = nextSeq-startSeqNum;// this will point to first data array index which is still need to send

		byte[] dataArray = new byte[segmentSize];

		MyTCPPacket newPacket = new MyTCPPacket();

		byte[] sendData;
		DatagramPacket sendPacket;
		ByteArrayOutputStream byteSendStream;
		ObjectOutputStream objectSendStream;

		try {
			//checking if the whole byte array is exhausted
			while(dataPointer < dataPacket.data.length){
				//chekcing if some bytes are available in window for sending
				if(nextSeq - base < windowSize){
					//getting data from the byte array
					if( dataPacket.data.length - dataPointer >= segmentSize){
						System.out.println("data size : " + dataPacket.data.length );
						dataArray  = Arrays.copyOfRange(dataPacket.data, dataPointer, dataPointer+segmentSize);
					}else{
						System.out.println("data size : " + dataPacket.data.length );
						dataArray = new byte[dataPacket.data.length - dataPointer];
						dataArray  = Arrays.copyOfRange(dataPacket.data, dataPointer, dataPacket.data.length);	
					}
					
					//forming and sending packet
					newPacket.data = dataArray;
					newPacket.setSeqNum(host_isn);
					newPacket.setAckNum(dest_isn);
					System.out.println(host_isn + " : " +dest_isn);
					byteSendStream = new ByteArrayOutputStream(20000);
					objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
					objectSendStream.flush();
					objectSendStream.writeObject(newPacket);
					objectSendStream.flush();
					sendData = byteSendStream.toByteArray();
					sendPacket = new DatagramPacket(sendData,sendData.length,destAddress,destPort);
					socket.send(sendPacket);
					objectSendStream.close();
					nextSeq++;
					host_isn = nextSeq;
					System.out.println("sent : " + dataArray.length);
					dataPointer = (nextSeq - startSeqNum)*segmentSize;
					Thread.sleep(10);
				}
			}

			//sending finish packet
			newPacket.data = new byte[0];
			newPacket.setSeqNum(host_isn);
			newPacket.setAckNum(dest_isn);
			newPacket.setFin();
			byteSendStream = new ByteArrayOutputStream(2000);
			objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
			objectSendStream.flush();
			objectSendStream.writeObject(newPacket);
			objectSendStream.flush();
			sendData = byteSendStream.toByteArray();
			sendPacket = new DatagramPacket(sendData,sendData.length,destAddress,destPort);
			socket.send(sendPacket);
			host_isn++;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while(!hearAck.getFinished()){
			
		}

	}

	//Method for listening to the data packets 
	public MyTCPPacket listen(){
		//Starting thread to process the packets side by side
		ProcessPackets pprocess = new ProcessPackets();
		Thread processingThread = new Thread(pprocess);
		processingThread.start();
		
		//intializing paramenter for receiving packet
		byte[] reqRecvData = new byte[20000];
		DatagramPacket recvPacket;
		ByteArrayInputStream byteRecvStream;
		ObjectInputStream objectRecvStream;
		recvPacket = new DatagramPacket(reqRecvData,reqRecvData.length);
		MyTCPPacket newRcvdPacket;
		try {
			do{
				System.out.println("Listening...");
				socket.receive(recvPacket);
				byteRecvStream = new ByteArrayInputStream(reqRecvData);
				objectRecvStream = new ObjectInputStream(new BufferedInputStream(byteRecvStream));
				newRcvdPacket = (MyTCPPacket)objectRecvStream.readObject();

				//Check for the validity of the packet
				if( newRcvdPacket.getSeqNum() == dest_isn && !newRcvdPacket.ack){
					//puch packet to the queue
					recvBuffer.add(newRcvdPacket);
					dest_isn++;

					//sending ack for this packet
					System.out.println("Send ack for this packet!");
					if(newRcvdPacket.fin){
						sendBackAck(true);
					}else{
						sendBackAck(false);
					}
				}else{
					System.out.println("dest_isn : "+ dest_isn);
				}


				System.out.println("recvd size : " + newRcvdPacket.data.length);

				System.out.println(newRcvdPacket.fin);
			}while(!newRcvdPacket.fin);
			keepOnProcessing = false;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MyTCPPacket finalPacket = pprocess.getFinalPacket(); 

		return finalPacket;
	}

	//Method for sending back acknowledgement for received packet
	public void sendBackAck(boolean fin){

		byte[] ackByte = new byte[0];

		MyTCPPacket newPacket = new MyTCPPacket();

		byte[] sendData;
		DatagramPacket sendPacket;
		ByteArrayOutputStream byteSendStream;
		ObjectOutputStream objectSendStream;

		try {
			//forming acknowledgemet packet and sending back
			newPacket.data = ackByte;
			newPacket.ack = true;
			newPacket.setSeqNum(host_isn);
			newPacket.setAckNum(dest_isn);
			if(fin){
				newPacket.setFin();
			}
			System.out.println(host_isn +" : "+dest_isn);
			byteSendStream = new ByteArrayOutputStream(20000);
			objectSendStream = new ObjectOutputStream(new BufferedOutputStream(byteSendStream));
			objectSendStream.flush();
			objectSendStream.writeObject(newPacket);
			objectSendStream.flush();
			sendData = byteSendStream.toByteArray();
			sendPacket = new DatagramPacket(sendData,sendData.length,destAddress,destPort);
			socket.send(sendPacket);
			objectSendStream.close();
			host_isn++;


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	//Threadd for listening to the acks which are sent for received packets
	class ListenAcks implements Runnable{
		public boolean finished = false;
		public void run() {
			byte[] reqAckData = new byte[20000];
			DatagramPacket ackPacket;
			ByteArrayInputStream byteAckStream;
			ObjectInputStream objectAckStream;
			ackPacket = new DatagramPacket(reqAckData,reqAckData.length);
			MyTCPPacket newRcvdPacket = null;
			
			//parameter for controlling timeout of the ack
			int timeOutVal = 1000;
			long startTime = 0;
			long endTime = 0;
			int rtt;
			try {

				do{
					try{
						
						//time for the acks to receive
						socket.setSoTimeout(timeOutVal);
						startTime = System.currentTimeMillis();
						System.out.println("Listening...Acks");
						endTime = System.currentTimeMillis();
						socket.receive(ackPacket);
						
						//forming and sending the ackpacket
						byteAckStream = new ByteArrayInputStream(reqAckData);
						objectAckStream = new ObjectInputStream(new BufferedInputStream(byteAckStream));
						newRcvdPacket = (MyTCPPacket)objectAckStream.readObject();
						if( newRcvdPacket.ack && newRcvdPacket.getSeqNum() == dest_isn){
							base = newRcvdPacket.getAckNum();
							System.out.println("RCVD ack : "+newRcvdPacket.getSeqNum()+" : "+newRcvdPacket.getAckNum());
							dest_isn++;
							if( windowSize < 10){
								windowSize++;
							}
						}else{
							if( windowSize > 1)
							windowSize /= 2;
							System.out.println("invalid ack : "+ newRcvdPacket.getSeqNum());
						}
					}catch(SocketException e){
						if( windowSize > 1)
						windowSize /= 2;
						System.out.println("timeoutAck");
					}
					
					//calculating new estimated RTT
					rtt = (int) (endTime-startTime);
					timeOutVal = calculateNewTime(timeOutVal,rtt);
				}while(!newRcvdPacket.fin);

				finished = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		
		//for checking if this thread is done
		public boolean getFinished(){
			return finished;
		}
		
		//calculating new estimated RTT
		int calculateNewTime(int timeOutVal, int rtt){
			return (int) (timeOutVal + 0.125*(rtt));
		}
	}

	
	//thread for processing the received packets
	class ProcessPackets implements Runnable{
		//arraylist to accumulated received data
		ArrayList<Byte> byteArray = new ArrayList<Byte>();
		MyTCPPacket curntPacket;
		public void run() {
			
			//setting flag on for the runnig state of this thread
			keepOnProcessing = true;
			while(keepOnProcessing){
				
				//checking queue if packet is available
				while( !recvBuffer.isEmpty()){
					curntPacket = recvBuffer.poll();
					for(int i=0; i < curntPacket.data.length; i++){
						byteArray.add(new Byte(curntPacket.data[i]));
					}
				}
			}
		}

		//making a final TCP packet and sending to the receiver.
		public MyTCPPacket getFinalPacket(){
			MyTCPPacket finalPacket = new MyTCPPacket();
			Byte[] newBytes = byteArray.toArray(new Byte[byteArray.size()]);
			finalPacket.data = new byte[newBytes.length];
			for(int i = 0 ; i < newBytes.length; i++){
				finalPacket.data[i] = newBytes[i].byteValue();
			}
			return finalPacket;
		}
	}
}
