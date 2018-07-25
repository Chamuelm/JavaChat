
/* 
 * File: Client.java
 * Client JFrame manages client side of chat 
 * 
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class Client extends JFrame {
	public static long connectionTimeout = 10000; // Timeout for connection

	private String serverAdd = "172.0.0.1"; // Holds server address
	private int serverPort = Server.serverPort; // Holds server port
	private JButton connectButton = new JButton("Connect");
	private JButton disconnectButton = new JButton("Disconnect");
	private JTextField enterField = new JTextField(); // User text field
	private JTextArea textArea = new JTextArea(); // Display area
	private boolean activeConnection = false; // Current connection state
	private Socket connection; // Connection to server
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ServerConnector serverConnector; // Process to connect to server
	private boolean keepAliveCondition = true; // Loop condition

	// Constructor
	public Client() {
		super("Client");

		// Build GUI north region
		JPanel northPanel = new JPanel();
		northPanel.add(connectButton);
		disconnectButton.setEnabled(false);
		northPanel.add(disconnectButton);
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Get server address and port from user
				serverAdd = (String) JOptionPane.showInputDialog(northPanel, "Please enter server address", "Connect",
						JOptionPane.INFORMATION_MESSAGE, null, null, "127.0.0.1");
				serverPort = Integer.parseInt((String) JOptionPane.showInputDialog(northPanel,
						"Please enter server port", "Connect", JOptionPane.INFORMATION_MESSAGE, null, null, 29860));

				setDisconnectButton(true); // Enables disconnect button

				serverConnector = new ServerConnector();
				serverConnector.execute();
			}
		});

		disconnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});

		add(northPanel, BorderLayout.NORTH);

		// Add text area
		textArea.setText("");
		textArea.setEditable(false);
		add(new JScrollPane(textArea), BorderLayout.CENTER);

		// Add input text field
		activeConnectionGUI(false); // disable gui components
		enterField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					sendData(event.getActionCommand());
				} catch (IOException e) {
					displayMessage("Error while trying to send data: " + event.getActionCommand());
				}
				enterField.setText("");
			}
		});
		add(enterField, BorderLayout.SOUTH);

		// Close connection when window closed
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				disconnect();
			}
		});

		setSize(300, 600);
		setVisible(true);
	}

	// Disconnect active connection
	private synchronized void disconnect() {
		if (activeConnection) {
			try {
				activeConnection = false;
				displayMessage("Closing connection...");
				activeConnectionGUI(false);

				// Inform other side and server to terminate connection
				if (output != null) {
					output.writeObject("SYSTEM-TERMINATE");
					output.flush();
				}

				// Wait for server and client to receive disconnect message
				wait(1500);
				closeConnection();
			} catch (IOException | InterruptedException e) {
				// Ignore
			}
		}
	}

	// Close active connection and streams
	private void closeConnection() throws IOException {
		displayMessage("Connection Terminated");
		if (output != null) {
			output.close();
			output = null;
		}
		if (input != null) {
			input.close();
			input = null;
		}
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	// Send data to server (to forward to partner)
	private void sendData(String message) throws IOException {
		output.writeObject(message);
		output.flush();
		displayMessage("You: " + message);
	}

	// Set gui component active/not active
	private void activeConnectionGUI(boolean editable) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				enterField.setEditable(editable);
				disconnectButton.setEnabled(editable);
				connectButton.setEnabled(!editable);
			}
		});
	}

	private void setDisconnectButton(boolean editable) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				disconnectButton.setEnabled(editable);
			}
		});
	}

	// Get input and output streams from socket
	private void getStreams() throws IOException {
		// Setup output stream for first participant
		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		// Setup input stream for first participant
		input = new ObjectInputStream(connection.getInputStream());
	}

	// Display message in text area
	private void displayMessage(String message) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				textArea.append(message + "\n");
			}
		});

	}

	private class ServerConnector extends SwingWorker<Void, Void> {
		protected Void doInBackground() throws IOException, ClassNotFoundException {
			Thread keepAlive;
			try {
				Timer timeoutTimer = new Timer();
				connection = new Socket(InetAddress.getByName(serverAdd), serverPort);
				activeConnection = true; // Flag connection as active
				getStreams();

				// Tell server that connection is still alive
				keepAlive = new Thread(new Runnable() {
					@Override
					public synchronized void run() {
						keepAliveCondition = true;
						while (keepAliveCondition) {
							try {
								if (output != null) {
									output.writeObject("KEEP-ALIVE");
									output.flush();
									wait(50);
								} else
									keepAliveCondition = false;

							} catch (IOException | InterruptedException e) {
								keepAliveCondition = false;
							}
						}
					}
				});
				keepAlive.start();

				displayMessage("Wait for partner to connect (" + connectionTimeout / 1000 + " seconds)");

				// Initialize timer for other player to connect
				timeoutTimer.schedule(new ConnectionCloser(), connectionTimeout);

				// Wait for message from server to announce successful connection
				displayMessage((String) input.readObject()); // First message from server
				timeoutTimer.cancel(); // Cancel timer if connection successful
				keepAliveCondition = false; // Cancel keep-alive messages
				activeConnectionGUI(true);

				String message = "";
				while (activeConnection) {
					try {
						message = (String) input.readObject(); // Read new message
						if (message.equals("SYSTEM-TERMINATE")) // Disconnect system message
							disconnect();
						else if (message.equals("KEEP-ALIVE"))
							;//ignore
						else
							displayMessage(String.format("Partner: %s", message));
					} catch (ClassNotFoundException | IOException e) {
						displayMessage("Execption1: " + e.getMessage());
						disconnect();
					}
				}
			} catch (IOException e) {
				// Connection cancelled
			}
			return null;
		}
	}

	private class ConnectionCloser extends TimerTask implements Runnable {

		@Override
		public void run() {
			try {
				if (activeConnection) {
					displayMessage("Timeout reached");
					closeConnection();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
