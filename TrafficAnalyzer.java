import java.util.ArrayList;
import java.io.IOException;
class TrafficAnalyzer
{
	String myIP;
	String otherParticipant;
	TrafficAnalyzer(String clientIP)
	{
		myIP = clientIP;
	}

	public static void main(String[] args)
	{
		TrafficAnalyzer analyzer;
		String notYetAnalyzed = new String();
		try
		{
			analyzer = new TrafficAnalyzer(args[0]);
		}catch(ArrayIndexOutOfBoundsException e)
		{
			System.out.println("Program requires your (internal) IP address as input, quitting");
			return;
		}
	}

	String readLine() throws IOException
	{
		byte latestByte = (byte)System.in.read();
		String outputString = new String();
		while(latestByte != 10 && latestByte != -1) outputString = outputString + latestByte;//10 is the line break character

		return outputString;
	}

	Packet[] checkForPackets(String toCheck)
	{
		
	}
}

class Packet
{
	String sender; //ip-address of the sender
	String recipient; //ip-address of the recipient
	ArrayList<Byte> contents;
	Packet(String sender, String recipient, ArrayList<Byte> contents)
	{
		this.sender = sender;
		this.recipient = recipient;
		this.contents = contents;
	}
}
