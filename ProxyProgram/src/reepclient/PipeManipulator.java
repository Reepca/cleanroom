package reepclient;
import java.net.Socket;
public interface PipeManipulator 
{
	public static PipeManipulator defaultBehavior = (b, from, to) -> 
	{
		return b;
	};
	byte[] actOnBytes(byte[] in, Socket from, Socket to);
}
