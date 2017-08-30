package networking.channels;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import networking.Connection;
import utils.Console;
import utils.ErrorType;

/**
 *
 * represents an abstract channel on a connection
 *
 */
public abstract class Channel {
	
	private String name;
	private Thread channel;
	
	protected Socket socket;
	protected Connection con;
	protected Console console;
	protected boolean ready;
	
	public Channel(String name) {
		this.name = name;
	}
	
	public void init(Connection con, Console console) {
		this.con = con;
		this.console = console;
	}
	
	/**
	 * abstract methods
	 */
	abstract void createIO();
	abstract void closeIO();
	public abstract ChannelType getType();
	
	/**
	 * starts the channel
	 * - sets up IO-Streams
	 * - start incoming listener
	 */
	public void start() {
		channel = new Thread(new Runnable() {
			public void run() {
				console.debug("Started channel " + name);
				try {
					socket = new Socket(con.getIP(), con.getPort());
					console.debug("Catched socket");
					createIO();
				} catch (UnknownHostException e) {
					console.error("Unknown host! ("+con.getIP()+":"+con.getPort()+")");
					con.lostConnection(ErrorType.SERVER_IS_OFFLINE);
					con.close();
					e.printStackTrace();
				} catch (IOException e) {
					console.error("Couldn't set up IO! ("+con.getIP()+":"+con.getPort()+")");
					con.lostConnection(ErrorType.SERVER_IS_OFFLINE);
					con.close();
				}
			}
		});
		channel.setName("CHANNEL_"+name);
		channel.start();
	}
	
	/**
	 * closes the channel and closes IO-Streams
	 */
	public void stop() {
		try {
			if (channel != null) channel.interrupt();
			closeIO();
			if (socket != null) socket.close();
		} catch (IOException e) {
			console.error("Could not close socket for channel: "+name);
			e.printStackTrace();
		}
	}
	
	/**
	 * getters
	 */
	
	public String getName() {
		return name;
	}
	
	public void waitLoading() {
		while (!ready) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}