/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ds.socketprograming;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
public class TCPServer {
 public static void main(String[] args) {
/* if (args.length < 1) {  // ......if argument lenth is less than 0.......
  System.err.println("Please provide the port number where to open the server");
  return;
  }
 int port = Integer.parseInt(args[0]);*/
int port=9999; // for connection , client and server needed same port number
 try {
 ServerSocket serverSocket = new ServerSocket(port);//creating a new ServerSocket object to listen on a specific port......
 System.out.println("Server is listening on port: " + port);
 Socket socket = serverSocket.accept(); //accept() Listens for a connection to be made to this socket and accepts it........
 System.out.println("New client connected...");
 DataInputStream dataInputStream = new 
DataInputStream(socket.getInputStream());//....InputStream() read for data form source file.......
                                          //getInputStream() returns an input stream for reading bytes from this socket.
 DataOutputStream dataOutputStream = new 
DataOutputStream(socket.getOutputStream());//....OutputStream() is used to write the data to destination file...
                                          //getOutputStream() returns an output stream for writing bytes to this socket. 
 String message = "";
 while (!message.equals("stop")) {
 message = dataInputStream.readUTF();
 System.out.println("Message from client: '" + message + "'");
 dataOutputStream.writeUTF(message);/* writeUTF() method of the java.io.DataOutputStream class accepts 
                                a String value as a parameter and writes it in using modified UTF-8 encoding, to the current output stream. */
 dataOutputStream.flush();
 }
 dataInputStream.close();
 socket.close();
 serverSocket.close();
 } catch (Exception ex) {
 ex.printStackTrace();
 }
}
}
