package bernhard.scharrer.netapi.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import bernhard.scharrer.netapi.packet.Packet;
import bernhard.scharrer.netapi.packet.Message;

class TCPChannel {

	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	private Thread receiver;
	private Console console;

	private Client client;
	private TCPModul listener;
	
	public TCPChannel(Client client, Socket socket, TCPModul listener, Console console) {
		
		this.client = client;
		this.console = console;
		this.listener = listener;
		
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			console.error("Stream broke down.");
			client.cleanUp();
		}
		
		startReceiver();
		
	}
	
	/**
	 * @param send message to client
	 */
	void send(final String message) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					out.writeObject(new Message(message));
					out.flush();
				} catch (IOException e) {
					console.error("Could not send message!");
					cleanUp();
				}
			}
			
		}).start();
	}
	
	/**
	 * @param send packet to client
	 */
	void send(final Packet packet) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					out.writeObject(packet);
					out.flush();
				} catch (IOException e) {
					console.error("Could not send packet!");
					cleanUp();
				}
			}
			
		}).start();
	}
	
	/**
	 * start receiving packets from clients
	 */
	private void startReceiver() {
		receiver = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Object obj;
				Packet packet;
				String msg;
				
				while (true) {
					try {
						obj = in.readObject();
						if (obj instanceof Packet) {
							packet = (Packet) obj;
							if (packet instanceof Message) {
								msg = (String) packet.getEntry("MSG");
								if (msg.startsWith("\r\r\r")) {
									String[] data = msg.split(";");
									if (data.length==4) {
										int port = Integer.parseInt(data[1]);
										int buffer = Integer.parseInt(data[2]);
										int cuid = Integer.parseInt(data[3]);
										console.debug("Setting up udp channel. (port: "+port+", buffer lengeth:"+buffer+", cuid:"+cuid+")");
										client.initUDP(port, buffer, cuid);
									} else console.warn("Server does not allow udp.");
								} else {
									listener.receive(msg);
								}
								msg = null;
							} else {
								console.debug("Incoming packet: "+packet.getName());
								listener.receive(packet);
							}
						} else {
							console.error("Strange packet! (Object: "+obj.toString()+")");
						}
					} catch (ClassNotFoundException e) {
						console.error("Unknown class! (Class: "+e.getClass().getName()+")");
						continue;
					} catch (IOException e) {
						console.debug("Stream broke down.");
						client.cleanUp();
						break;
					}
				}
			}
			
		});
		
		receiver.start();
	}

	public void cleanUp() {
		listener.disconnect();
		receiver.interrupt();
		console.debug("Cleaning up channel.");
	}
	
}
