
/* 
 * File: Server.java
 * Server to accept and manage connections and chat conversation 
 * 
 */
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	public static int serverPort = 29860;
	public static int maxConnections = 100;

	private ServerSocket server;
	private ExecutorService executor = Executors.newFixedThreadPool(maxConnections / 2); // One thread for 2 connections
	private Socket connection1;
	private Socket connection2;
	private int conversationsCount = 1;

	public Server() {
		try {
			server = new ServerSocket(serverPort, maxConnections);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// Start waiting for connections
	public void startServer() {
		while (true) {
			waitForConnections();
			conversationsCount++;
		}
	}

	// Wait for 2 connections and setup a conversation between them
	private void waitForConnections() {
		Object[] streams = new Object[4];
		boolean keepLookForPartner = false;
		try {
			System.out.println("Waiting for connections...");
			connection1 = server.accept();
			// Get streams
			streams[0] = new ObjectOutputStream(connection1.getOutputStream());
			((ObjectOutputStream) streams[0]).flush();
			streams[1] = new ObjectInputStream(connection1.getInputStream());

			System.out.println("Connection " + conversationsCount + ", 1st participant: "
					+ connection1.getInetAddress().getHostName());

			do {
				System.out.println("Waiting for 2nd participant...");
				connection2 = server.accept();
				// Get streams
				streams[2] = new ObjectOutputStream(connection2.getOutputStream());
				((ObjectOutputStream) streams[2]).flush();
				streams[3] = new ObjectInputStream(connection2.getInputStream());

				System.out.println("Connection " + conversationsCount + ", 2nd participant: "
						+ connection2.getInetAddress().getHostName());
				keepLookForPartner = false;

				try {
					checkConnection((ObjectInputStream) streams[1]);
				} catch (IOException e) {
					// First partner disconnected before second connected
					System.out.println("Connection " + conversationsCount + ", 1st participant disconnected.");
					connection1 = connection2;
					streams[0] = streams[2];
					streams[1] = streams[3];
					keepLookForPartner = true;
				}
			} while (keepLookForPartner);
			checkConnection((ObjectInputStream) streams[3]);

		} catch (EOFException e) {
			// Ignore
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Add thread to manage this conversation
		executor.execute(new Conversation(connection1, connection2, conversationsCount, streams));
	}

	boolean checkConnection(ObjectInputStream inputStream) throws IOException {
		long start = System.currentTimeMillis();
		long timeToCheck = 1000;
		while (System.currentTimeMillis() - start < timeToCheck)
			try {
				inputStream.readObject();
			} catch (ClassNotFoundException e) {
				// Ignore
			}
		return true;
	}
}
