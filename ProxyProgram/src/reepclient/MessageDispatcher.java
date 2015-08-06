package reepclient;
import java.net.Socket;

public class MessageDispatcher 
{
	Socket inputSocket;
	Socket outputSocket;
	ProxyProgram dispatchTo;
	Thread listenerThread = new Thread(() ->
	{
		while(!inputSocket.isClosed() && !outputSocket.isClosed())
		{
			try
			{
				dispatchTo.addSocketMessage(getNextMessage());
			}catch(Exception e)
			{
				System.out.println("Something went wrong while reading from " + 
								   inputSocket.toString() + ". Message and stack trace follow: ");
				System.out.println(e.getMessage());
				e.printStackTrace();
				break;
			}
		}
		try
		{
			inputSocket.close();
			outputSocket.close();
		}catch(Exception e){}
		
	});
	
	
	MessageDispatcher(Socket inputSocket, Socket outputSocket, ProxyProgram dispatchTo)
	{
		this.inputSocket = inputSocket;
		this.outputSocket = outputSocket;
		this.dispatchTo = dispatchTo;
		listenerThread.start();
	}
	

	public SocketMessage getNextMessage()throws Exception
	{
		byte[] bytesIn = new byte[inputSocket.getInputStream().available() + 5000];
		int byteCount = inputSocket.getInputStream().read(bytesIn);
		if(byteCount > -1)
		{
			byte[] bytesRead = new byte[byteCount];
			System.arraycopy(bytesIn, 0, bytesRead, 0, bytesRead.length);
			return new SocketMessage(bytesRead, inputSocket, outputSocket, System.currentTimeMillis());
		}
		return null;
	}
	
	public Socket getInSocket()
	{
		return inputSocket;
	}
	
	public Socket getOutSocket()
	{
		return outputSocket;
	}
}