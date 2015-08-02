package reepclient;



import java.net.InetAddress;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.File;
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
	ArrayList<Socket> serverSideSockets = new ArrayList<Socket>();
	ArrayList<Socket> clientSideSockets = new ArrayList<Socket>();
	PrintStream output = System.out;
	
	PipeManipulator callProcessingChain = (throughBytes, from, to) ->
	{
		byte[] out = throughBytes;
		for(PipeManipulator thisAction: processingChain)
		{
			out = thisAction.actOnBytes(out, from, to);
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
				serverSideSockets.add(outToServerConnection);
				clientSideSockets.add(newClientConnection);
				synchronized(output)
				{
					output.println("new client connection: " + 
										newClientConnection.toString().replace("Socket", ""));
					output.println("Successfully connected to server, Socket information: " +
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
	}
	
	public ArrayList<Socket> getClientSideSockets()
	{
		return clientSideSockets;
	}
	
	public ArrayList<Socket> getServerSideSockets()
	{
		return serverSideSockets;
	}
	
	public ArrayList<Socket> getAllSockets()
	{
		ArrayList<Socket> returnList = new ArrayList<Socket>();
		returnList.addAll(clientSideSockets);
		returnList.addAll(serverSideSockets);
		return returnList;
	}
	
	public void startListening()
	{
		clientGreeter.start();
		System.out.println("now listening for connections on port " + inFromClientPort);
	}
	
	public void setPrintStream(PrintStream setTo)
	{
		output = setTo;
	}
	
	public PrintStream getPrintStream()
	{
		return output;
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
		String programCalledFromDir = System.getProperty("user.dir");
			
		ProxyProgram proxy = new ProxyProgram(PRIMARY_CLIENT_PORT, OUT_PORT, WORLD_60_GAME_SERVER, 
				chainOfProcessing);
		
		PipeManipulator byteRawOut = (throughBytes, from, to) ->
		{
			try
			{
				proxy.getPrintStream().write(throughBytes);
			}catch(IOException e)
			{
				System.err.println("Something went wrong writing to PrintStream");
				e.printStackTrace();
			}
			return throughBytes;
		};
		
		PipeManipulator bytePrinter = (throughBytes, from, to) ->
		{
			if(throughBytes.length > 0)
			{			
				long timeStamp = System.currentTimeMillis();
				PrintStream output = proxy.getPrintStream();
				synchronized(output)
				{
					String alias;
					if(proxy.isServerSide(from))alias = "server";
					else alias = "client";
					
					output.println("from " + alias + " " + from.toString().replace("Socket", "") + ": ");
					output.println("timestamp: " + timeStamp);
					int timesPrinted = 0;
					int perLineLimit = 10;
					for(byte b: throughBytes)
					{
						output.print(" " + b + " ");
						timesPrinted++;
						if(timesPrinted % perLineLimit == 0)
						{
							output.println();
						}
					}
					output.println();
				}
			}
			return throughBytes;
		};
		
		PipeManipulator textPrinter = (throughBytes, from, to) ->
		{
			if(throughBytes.length > 0)
			{
				PrintStream output = proxy.getPrintStream();
				synchronized(output)
				{
					output.println("text representation: ");
					int perLineLimit = 20;
					String fullString = new String(throughBytes);
					int sectionCount = (int)Math.ceil((float)fullString.length() / (float)perLineLimit);
					for(int i = 0; i < sectionCount; i++)
					{
						if(i != sectionCount - 1)
						{
							output.println(fullString.substring(i * perLineLimit, (i + 1) * perLineLimit));
						}else
						{
							output.println(fullString.substring(i * perLineLimit, fullString.length() - 1));
						}
					}
					output.println();
				}
			}
			return throughBytes;
		};
		
		Thread inputParser = new Thread(() -> 
		{
			while(true)
			{
				try
				{
					byte[] input = new byte[1000];
					System.in.read(input);
					String inputString = new String(input).trim();
					String[] separatedInput = inputString.split(" ");
					for(int i = 0; i < separatedInput.length; i++)
					{
						if(separatedInput[i].equals("--output") && i != separatedInput.length - 1)
						{
							synchronized(proxy.getPrintStream())
							{
								proxy.getPrintStream().flush();
								if(!separatedInput[i+1].toLowerCase().trim().equals("system.out"))
								{
									
									File writeOutTo = new File((separatedInput[i+1]).trim());
									
									writeOutTo.createNewFile();
									//System.out.println("successfully made File");
									proxy.setPrintStream(new PrintStream(writeOutTo));
								}else proxy.setPrintStream(System.out);
							}
							i++;
						}
						
						if(separatedInput[i].toLowerCase().equals("newmessage") && separatedInput.length - i > 1)
						{
							String sendTo = separatedInput[i + 1];
							String[] bytes = separatedInput[i + 2].split(",");
							byte[] byteValues = new byte[bytes.length];
							for(int j = 0; i < bytes.length; i++)
							{
								byteValues[j] = Byte.parseByte(bytes[j]);
							}
							
							proxy.getAllSockets().stream().filter(testSocket -> 
							(testSocket.getInetAddress().getHostName().equals(sendTo) || testSocket.
								getInetAddress().getHostAddress().equals(sendTo))).forEach(s ->
								{
									try
									{
										s.getOutputStream().write(byteValues);
									}catch(Exception e)
									{
											System.out.println("Something went wrong while sending " +
															   "custom message to " + s.getInetAddress()
															   .getHostName());
											e.printStackTrace();
									}
								});
							
						}
							
					}
				}catch(Exception e)
				{
					System.err.println("Something went wrong while parsing the input: " + e.getMessage());
				}
			}
		});
		TimeDisplay timeShower = new TimeDisplay(600);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {proxy.getPrintStream().flush();}));
		inputParser.start();
		String[] pipeStrings = {"-binary", "-bytes", "-text"};
		PipeManipulator[] pipes = {byteRawOut, bytePrinter, textPrinter};
		for(int i = 0; i < pipeStrings.length; i++)
		{
			for(int j = 0; j < args.length; j++)
			{
				if(args[j].toLowerCase().equals(pipeStrings[i]))
				{
					chainOfProcessing.add(pipes[i]);
					break;
				}
			}
		}
		if(chainOfProcessing.size() < 2){chainOfProcessing.add(bytePrinter);}
			
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
