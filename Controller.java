/* 
 * File: Controller.java
 * Controller to enable waiting for threads to do their jobs. 
 * 
 */
public class Controller {
	private int num; // Number of threads to wait for
	private int count = 0; // Number of threads finished

	public Controller(int num) {
		this.num = num;
	}

	// Called by finished thread to announce it finished
	public synchronized void finished() {
		count++;
		if (count >= num) {
			notifyAll();
		}
	}

	// Called by main where waiting for threads to finish
	public synchronized void waitForThreads() {
		while (count < num)
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("Interrupted while waiting.");
			}
	}

	// Called by finished thread to announce it finished and to wait for other
	// threads
	public synchronized void finishedAndWait() {
		count++;
		if (count >= num) {
			notifyAll();
		} else {
			while (count < num)
				try {
					wait();
				} catch (InterruptedException e) {
					System.out.println("Interrupted while waiting.");
				}
		}
	}

	// Return current count
	public synchronized int getCount() {
		return count;
	}
}
