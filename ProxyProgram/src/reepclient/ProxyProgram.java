package reepclient;
// TODO: Convert full byte sequences to strings to print, for better unicode support
//TODO: Add pseudo-regex-ish support to findByteSequence - probably add a helper method.

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
	ArrayList<PipeManipulator> processingChain;
	
	PipeManipulator callProcessingChain = (throughBytes, from, to) ->
	{
		byte[] out = null;
		for(PipeManipulator thisAction: processingChain)
		{
			out = thisAction.actOnBytes(throughBytes, from, to);
		}
		return out;
	};
	
	
	Thread clientGreeter = new Thread(() ->
	{
		while(true)
		{
			try
			{
				Socket newClientConnection = listeningServer.accept();
				Socket outToServerConnection = new Socket(serverAddress, outToServerPort);
				pipes.add(new SocketPipe(newClientConnection, outToServerConnection, callProcessingChain));
				pipes.add(new SocketPipe(outToServerConnection, newClientConnection, callProcessingChain));
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
	

	ProxyProgram(int inFromClientPort, 
				 int outToServerPort, 
				 String serverAddress,
				 ArrayList<PipeManipulator> processingChain)throws Exception
	{
		this.serverAddress = serverAddress;
		this.inFromClientPort = inFromClientPort;
		this.outToServerPort = outToServerPort;
		this.processingChain = processingChain;
		processingChain.add(PipeManipulator.defaultBehavior);
		listeningServer = new ServerSocket(inFromClientPort);
		System.out.println("now listening for connections on port " + inFromClientPort);
	}
	
	public void startListening()
	{
		clientGreeter.start();
	}
	
	ArrayList<PipeManipulator> getProcessingChain()
	{
		return processingChain;
	}
	
	public boolean isServerSide(Socket testSocket)
	{
		if(testSocket.getPort() == outToServerPort) return true;
		return false;
	}
	
	public static void main(String[] args)throws Exception
	{
		ArrayList<PipeManipulator> chainOfProcessing = new ArrayList<PipeManipulator>();
		ProxyProgram proxy = new ProxyProgram(PRIMARY_CLIENT_PORT, OUT_PORT, "oldschool60.runescape.com", 
				chainOfProcessing);
		PipeManipulator printer = (throughBytes, from, to) ->
		{
			if(throughBytes.length > 0)
			{			
				long timeStamp = System.currentTimeMillis();
				synchronized(System.out)
				{
					String alias;
					if(proxy.isServerSide(from))alias = "server";
					else alias = "client";
					
					System.out.println("from " + alias + " " + from.toString().replace("Socket", "") + ": ");
					System.out.println("timestamp: " + timeStamp);
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
		chainOfProcessing.add(printer);
		proxy.startListening();
		
	}
	/**
	 * 
	 * @param bytes bytes through which to search for a sequence
	 * @param sequence a sequence of bytes to search for
	 * @return -1 if no occurrence of the sequence is found, or the index of the beginning of
	 * where the sequence occurs
	 */
	public static int findByteSequence(int startIndex, byte[] bytes, ByteMatcher[] sequence)
	{
		if(bytes.length > sequence.length)
		{
			for(int i = startIndex; i < bytes.length; i++)
			{
				for(int j = 0; j < sequence.length; j++)
				{
					if(i + sequence.length > bytes.length) break;
					
					if(!sequence[j].byteMatches(bytes[i + j])) break;
					
					if(j == sequence.length - 1) return i;
				}
			}
		}
		return -1;
	}
	
	public static byte[] findAndReplaceByteSequence(byte[] bytes, ByteMatcher[] sequence, byte[] replaceWith)
	{
		ArrayList<Integer> occurrences = new ArrayList<Integer>();
		int startIndex = 0;
		while(startIndex != -1)
		{
			startIndex = findByteSequence(startIndex, bytes, sequence);
			if(startIndex != -1)
			{
				occurrences.add(new Integer(startIndex));
				startIndex++;
			}
		}
		byte[] fullOutput = new byte[bytes.length + (replaceWith.length - sequence.length)];
		int previousOccurrence = 0;
		for(int i = 0; i < occurrences.size(); i++)
		{
			if(i == 0)
			{
				System.arraycopy(bytes, previousOccurrence, fullOutput, previousOccurrence, occurrences.get(i).intValue());
			}else
			{
				System.arraycopy(bytes, previousOccurrence, 
						fullOutput, previousOccurrence + i * (replaceWith.length - sequence.length), 
						occurrences.get(i).intValue() - previousOccurrence);
			}
			for(int j = 0; j < replaceWith.length; j++)
			{
				fullOutput[occurrences.get(i) + j] = replaceWith[j];
			}
			previousOccurrence = occurrences.get(i).intValue();
		}
		if(occurrences.size() > 0)
		{
			System.arraycopy(bytes, previousOccurrence, fullOutput,
					previousOccurrence + occurrences.size()
					* (replaceWith.length - sequence.length), bytes.length - previousOccurrence);
		}
		return fullOutput;
	}
}
