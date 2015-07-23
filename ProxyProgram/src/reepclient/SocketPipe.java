package reepclient;
import java.net.Socket;

public class SocketPipe 
{
	Socket inputSocket;
	Socket outputSocket;
	PipeManipulator throughAction;
	Thread piperThread = new Thread(() ->
	{
		while(!inputSocket.isClosed() && !outputSocket.isClosed())
		{
			try
			{
				pipeThroughSockets();
			}catch(Exception e)
			{
				System.out.println("Something went wrong while reading from " + 
								   inputSocket.toString() + ". Message and stack trace follow: ");
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		try
		{
			inputSocket.close();
			outputSocket.close();
		}catch(Exception e){}
		
	});
	
	SocketPipe(Socket inputSocket, Socket outputSocket)
	{
		this.inputSocket = inputSocket;
		this.outputSocket = outputSocket;
		this.throughAction = PipeManipulator.defaultBehavior;
		piperThread.start();
	}
	
	SocketPipe(Socket inputSocket, Socket outputSocket, PipeManipulator throughAction)
	{
		this.inputSocket = inputSocket;
		this.outputSocket = outputSocket;
		if(throughAction != null)
		{
			this.throughAction = throughAction;
		}else this.throughAction = PipeManipulator.defaultBehavior;
		
		piperThread.start();
	}
	
	/**
	 * Reads bytes from this SocketPipe's input, acts on them using this SocketPipe's
	 * PipeManipulator, and then writes the result to this SocketPipe's output.
	 * @return the bytes that were read from the input Socket and sent to the output Socket
	 */
	public byte[] pipeThroughSockets()throws Exception
	{
		byte[] bytesIn = new byte[inputSocket.getInputStream().available() + 5000];
		int byteCount = inputSocket.getInputStream().read(bytesIn);
		if(byteCount > -1)
		{
			byte[] bytesRead = new byte[byteCount];
			System.arraycopy(bytesIn, 0, bytesRead, 0, bytesRead.length);
			bytesRead = throughAction.actOnBytes(bytesRead, inputSocket, outputSocket);
			outputSocket.getOutputStream().write(bytesRead);
			return bytesRead;
		}
		return new byte[0];
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