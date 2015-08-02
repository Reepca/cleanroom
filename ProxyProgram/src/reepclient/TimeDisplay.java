package reepclient;

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TimeDisplay extends Frame 
{
	volatile boolean running = true;
	long timePerUpdate;
	String timestamp;
	Thread displayUpdater = new Thread(() ->
	{
		while(running)
		{
			timestamp = String.valueOf(System.currentTimeMillis());
			repaint();
			try{Thread.sleep(timePerUpdate);}
			catch(InterruptedException e){System.out.println("interrupted"); }
		}
		dispose();
	});
	
	TimeDisplay(float updatesPerSecond)
	{
		super("Current time (millis)");
		timePerUpdate = (long)(1000.0f / updatesPerSecond);
		addWindowListener(new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
						running = false;
					}
				});
		
		addNotify();
		setVisible(true);
		setResizable(true);
		setSize(300, 100);
		setLocation(new java.awt.Point(500, 500));
		displayUpdater.start();
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		g.clearRect(0, 0, getWidth(), getHeight());
		g.drawString(timestamp, (getWidth() / 2), (getHeight() / 2));
	}
}
