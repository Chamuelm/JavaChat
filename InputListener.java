/* 
 * File: InputListener.java
 * Object to manage messaging between received input and output streams 
 * 
 */
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class InputListener implements Runnable {
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private Controller controller;		// Controller to manage disconnection of both listeners 

	public InputListener(ObjectOutputStream output, ObjectInputStream input, Controller controller) {
		this.output = output;
		this.input = input;
		this.controller = controller;
	}

	@Override
	public void run() {
		String message = "";
		while (controller.getCount() == 0) { // While both sides are still active
			try {
				message = (String) input.readObject(); // Read new message
				if (message.equals("SYSTEM-TERMINATE")) // Connection termination system message
					controller.finished();
				output.writeObject(message); // Send to receiver
				output.flush(); // Flush output
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}
