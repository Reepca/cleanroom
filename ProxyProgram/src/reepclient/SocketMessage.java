package reepclient;
import java.net.Socket;

public class SocketMessage 
{
	public byte[] byteContent;
	public Socket from;
	public Socket to;
	
	SocketMessage(byte[] message, Socket from, Socket to)
	{
		this.byteContent = message;
		this.from = from;
		this.to = to;
	}
	
	public void sendMessage() throws Exception
	{
		to.getOutputStream().write(byteContent);
	}
}