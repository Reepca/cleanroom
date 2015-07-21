package reepclient;
import java.net.InetAddress;
import java.util.ArrayList;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
public class ProxyProgram 
{
	public static final int PRIMARY_CLIENT_PORT = 43594;
	public static final int UPDATE_SERVER_IN_PORT = 43596;
	public static final String WORLD_60_GAME_SERVER = "oldschool60.runescape.com";
	public static final String UPDATE_SERVER = "";
	
	//has to be different from client port so it doesn't get sent back to itself
	public static final int OUT_PORT = 43333; 
	
	int inFromClientPort;
	int outToServerPort;
	String serverAddress;
	ServerSocket listeningServer;
	ArrayList<SocketPipe> pipes = new ArrayList<SocketPipe>();
	
	PipeManipulator printer = (throughBytes, from, to) ->
	{
		if(throughBytes.length > 0)
		{
			synchronized(System.out)
			{
				String alias;
				if(from.getPort() == outToServerPort)alias = "server";
				else alias = "client";
				
				System.out.println("from " + alias + " " + from.toString().replace("Socket", "") + ": ");
				int timesPrinted = 0;
				int perLineLimit = 10;
				for(byte b: throughBytes)
				{
					System.out.print(" " + b + " ");
					timesPrinted++;
					if(timesPrinted % perLineLimit == 0)
					{
						System.out.println();
					}
				}
				System.out.println();
				System.out.println("text representation: ");
				timesPrinted = 0;
				perLineLimit = 20;
				for(byte b: throughBytes)
				{
					System.out.print(new String(new byte[]{b}));
					timesPrinted++;
					if(timesPrinted % perLineLimit == 0)
					{
						System.out.println();
					}
				}
				System.out.println();
				
			}
		}
		return throughBytes;
	};
	
	
	Thread clientGreeter = new Thread(() ->
	{
		while(true)
		{
			try
			{
				Socket newClientConnection = listeningServer.accept();
				Socket outToServerConnection = new Socket(serverAddress, outToServerPort);
				pipes.add(new SocketPipe(newClientConnection, outToServerConnection, printer));
				pipes.add(new SocketPipe(outToServerConnection, newClientConnection, printer));
				synchronized(System.out)
				{
					System.out.println("new client connection: " + 
										newClientConnection.toString().replace("Socket", ""));
					System.out.println("Successfully connected to server, Socket information: " +
									   outToServerConnection.toString().replace("Socket", ""));
				}
			}catch(Exception e)
			{
				System.err.println("Something went wrong while accepting a new client");
			}
		}
	});
	

	ProxyProgram(int inFromClientPort, int outToServerPort, String serverAddress)throws Exception
	{
		this.serverAddress = serverAddress;
		this.inFromClientPort = inFromClientPort;
		this.outToServerPort = outToServerPort;
		listeningServer = new ServerSocket(inFromClientPort);
		System.out.println("now listening for connections on port " + inFromClientPort);
		clientGreeter.start();
	}
	
	public static void main(String[] args)throws Exception
	{
		ProxyProgram proxy = new ProxyProgram(PRIMARY_CLIENT_PORT, OUT_PORT, "oldschool60.runescape.com");
		
	}
	
	
}
