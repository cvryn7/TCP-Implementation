import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
/*
 * This class acts as client for receiving the the 
 * file being sent by server.
 * 
 * Author : Karan Bhagat
 * Modified : 8 Nov 2015
 */
public class Client {

	//for making reliable TCP connection
	MyTCPSocket socket;

	//Main method
	public static void main(String[] args) {
		//Getting reference of this class for calling non static fields
		Client clnt = new Client();
		try {
			
			//intializing socket
			clnt.socket = new MyTCPSocket(6565,InetAddress.getByName(args[0]), Integer.parseInt(args[1]));
			System.out.println("handshake done!!!");
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		//Requesting name of the file from server
		clnt.requestFile("sendfileName");
		MyTCPPacket rcvdPacket = clnt.socket.listen();
		
		//Requesting file data from the server
		String filename = new String(rcvdPacket.data);
		clnt.requestFile("sendfile");

		//getting received file data into byte array
		byte[] fileData = (clnt.socket.listen()).data;

		//writing received file data to the file
		FileOutputStream fileOuputStream;
		try {
			fileOuputStream = new FileOutputStream("x"+filename);
			fileOuputStream.write(fileData);
			fileOuputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	//method to send request msg to server
	void requestFile(String rqstMsg){
		byte[] rqstBytes = rqstMsg.getBytes();
		MyTCPPacket rqstPacket = new MyTCPPacket(rqstBytes);
		socket.send(rqstPacket);
	}

}
