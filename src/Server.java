import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
/*
 * This class mimic the functionality of the server
 * which send file to the client
 * 
 * Author : Karan Bhagat
 * Modified : 8 Nov 2015
 */
public class Server {


	MyTCPServerSocket myServerSocket;//server socket
	MyTCPSocket clientSocket;//client socket
	
	//main program
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Server myServer = new Server();	
		
		myServer.myServerSocket = new MyTCPServerSocket(6565);
		
		//initiating handshake
		myServer.clientSocket = myServer.myServerSocket.accept();
		System.out.println("got client socket..");
		MyTCPPacket getRqstPacket;
		String s;

		//listening for client request
		getRqstPacket = myServer.clientSocket.listen();
	
		s = new String(getRqstPacket.data);
		System.out.println("Now sending filename : "+s);
		if( s.equals("sendfileName")){
			//sending file name to client
			myServer.sendFileName(args[0]);
		}
		//listening for client request
		getRqstPacket = myServer.clientSocket.listen();
		s = new String(getRqstPacket.data);
		if( s.equals("sendfile")){
			//sending file to the client
			myServer.sendFile(args[0]);
		}

	}

	//Method for retrieving file and sending it
	void sendFile(String filename){
		Path path = Paths.get(filename);
		byte[] fileData = null;
		try {
			fileData = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
		MyTCPPacket filePacket = new MyTCPPacket(fileData);
		clientSocket.send(filePacket);	
	}
	
	//Method for sending file name to the client
	void sendFileName(String filename){
		byte[] fileNameBytes = filename.getBytes();
		MyTCPPacket rqstPacket = new MyTCPPacket(fileNameBytes);
		clientSocket.send(rqstPacket);
	}


}
