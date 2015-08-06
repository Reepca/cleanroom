package reepclient;
import java.net.Socket;

public class SocketMessage 
{
	public byte[] byteContent;
	public Socket from;
	public Socket to;
	public long timeOfArrival;
	
	SocketMessage(byte[] message, Socket from, Socket to, long timeOfArrival)
	{
		this.byteContent = message;
		this.from = from;
		this.to = to;
		this.timeOfArrival = timeOfArrival;
	}
	
	public void sendMessage() throws Exception
	{
		to.getOutputStream().write(byteContent);
	}
}