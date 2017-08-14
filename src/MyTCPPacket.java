import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;

/*
 * Class to encapsulate data in a tcp packet
 * 
 * Author : Karan Bhagat
 * Modified : 8 Nov 2015
 */
public class MyTCPPacket implements Serializable {

	
	byte[] data;//data to send
	int seqNum;//sequenct number of packet
	int ackNum;//acknowlegment number expecting
	int recvWindow;//receiving window available
	boolean ack=false;//if this is ack packet
	boolean fin=false;//if this is finish packet
	boolean syn=false;//if this is handshake packet
	int checkSum;
	
	//empty constructor
	MyTCPPacket(){
		
	}
	//constructor for initialize with sequence number
	MyTCPPacket(int seqNum){
		this.seqNum = seqNum;
	}
	
	//Constructor for intializing with seq number and syn bit
	MyTCPPacket(int seqNum, boolean syn){
		this.seqNum = seqNum;
		this.syn = syn;
	}
	
	//Constructor
	MyTCPPacket(int seqNum, boolean syn, boolean ack, int ackNum){
		this.seqNum = seqNum;
		this.syn = syn;
		this.ack = ack;
		this.ackNum = ackNum;
	}
	//Constructor
	MyTCPPacket(int seqNum, boolean ack, int ackNum){
		this.seqNum = seqNum;
		this.ack = ack;
		this.ackNum = ackNum;
	}
	//constructor
	MyTCPPacket(byte[] data){
		
		this.data = data;
	}
	
	//Method to set syn bit
	public void setSyn(){
		syn = true;
	}
	
	//Method to get sequence number
	public int getSeqNum(){
		return seqNum;
	}
	
	//Method to set sequence number
	public void setSeqNum(int seqNum){
		this.seqNum = seqNum;
	}
	
	//Method to set acknowledgement number
	public void setAckNum(int ackNum){
		this.ackNum = ackNum;
	}
	
	//Method to set finish bit
	public void setFin(){
		fin = true;
	}
	
	//Method to get acknowledgement number
	public int getAckNum(){
		return ackNum;
	}
}
