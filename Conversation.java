/* 
 * File: Conversation.java
 * Manages conversation between 2 connections to server
 * 
 */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Conversation implements Runnable {
	private Socket connection1;		// Connection to first client
	private Socket connection2;		// Connection to second client
	private int conversationNumber; // Conversation ID
	private ObjectOutputStream con1Output;	// Output stream for connection1
	private ObjectInputStream con1Input;	// Input stream for connection1
	private ObjectOutputStream con2Output;	// Output stream for connection2
	private ObjectInputStream con2Input;	// Input stream for connection2
	private ExecutorService executor = Executors.newFixedThreadPool(2);
	private Controller controller; // Control if one thread has finished	

	public Conversation(Socket connection1, Socket connection2, int conversationNumber, Object[] streams) {
		this.connection1 = connection1;
		this.connection2 = connection2;
		this.conversationNumber = conversationNumber;
		controller = new Controller(2);
		getStreams(streams);
	}

	@Override
	public void run() {
		// Create 2 InputListener to forward messages between sides and manage disconnection
		try {
			announceConnection(); // Send connection details to both sides and server console
			executor.execute(new InputListener(con1Output, con2Input, controller));
			executor.execute(new InputListener(con2Output, con1Input, controller));
			executor.shutdown();
			controller.waitForThreads(); // Wait for both sides to finish connection
			closeConnection();			// Close connection and streams
		} catch (ConnectException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Get streams from both connections
	private void getStreams(Object[] streams) {
		con1Output = (ObjectOutputStream)streams[0];
		con1Input = (ObjectInputStream)streams[1];
		con2Output = (ObjectOutputStream)streams[2];
		con2Input = (ObjectInputStream)streams[3];
	}

	// Inform server console and both clients about connection details
	private void announceConnection() throws IOException {
		// Announce connection in server screen
		System.out.println("Conversation " + conversationNumber + " started.");
		// Announce connection in clients
		con1Output.writeObject(String.format("Connection started with %s.", connection2.getInetAddress().getHostName()));
		con1Output.flush();
		con2Output.writeObject(String.format("Connection started with %s.", connection1.getInetAddress().getHostName()));
		con2Output.flush();
	}
	
	// Close connections and streams
	private void closeConnection() throws IOException {
		con1Output.close();
		con2Output.close();
		con2Input.close();
		con1Input.close();
		connection1.close();
		connection2.close();
		System.out.println("Conversation " + conversationNumber + " has been disconnected.");
	}

}
