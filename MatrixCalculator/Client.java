import java.net.*;
import java.io.*;
import java.util.Arrays;

/**
 * Client to connect to server for matrix calculations
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class MatrixClient {
	
	public static void main(String[] args) throws IOException {
		
		System.out.println("Create instances of MatrixClient and call 'calculate' to connect to server");
		
		// Nothing to do here!
		// See driver class for simulating client connections.
	}
	
	// Starts a client thread to communicate with the server, in order to perform
	// a matrix calculation with size 'size' and partitioning method 'operation'
	// Returns a 'Result' object which will include a matrix if returnMatrix is true.
	public static Result calculate(Operation operation, int size, boolean returnMatrix) {
		Result result;
		
		// Create thread to connect to server, wait for it to finish
		ClientThread t = new ClientThread(operation, size, returnMatrix); 
		try {
			t.join();
		} catch (InterruptedException e) {
			System.out.println("Could not join thread, no answer will be returned.");
			return(new Result(null, -1));
		}
		
		result = t.result; // Collect result from ClientThread
		
		// Return result, printing matrix if possible.
		System.out.println("Call to 'calculate' completed with error code " + result.errorCode);
		if (result.errorCode >= 0) {
			if (returnMatrix) {
				HelperMethods.printMatrix(result.answer);
			} else {
				System.out.println("Matrix return not requested");
			}
		}
		return(t.result);
	}
}


/**
 * Thread for one connection to server (one calculation)
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class ClientThread extends Thread {
	
	final int PORT_NUMBER = 8080;
	
	Operation operation;
	int size;
	boolean returnMatrixResult;
	Result result;
	
	public ClientThread(Operation op, int size, boolean rmr) {
		this.operation = op;
		this.size = size;
		this.returnMatrixResult = rmr;
		start();
	}
	
	public void run() {
		DataOutputStream out = null;
		ObjectInputStream inob = null;
		Socket socket = null;
		
		try {
			// Send required information to server, so that calculation can be performed
			InetAddress addr = InetAddress.getByName(null);
			socket = new Socket(addr, PORT_NUMBER); // The connection to the server
			out = new DataOutputStream(socket.getOutputStream());
			out.writeInt(size);
			out.writeInt(operation.ordinal());
			out.writeBoolean(returnMatrixResult);
			inob = new ObjectInputStream(socket.getInputStream());
			
			// Read result of the computation from the server.
			try {
				result = (Result) inob.readObject();
			} catch (ClassNotFoundException cnf) {
				System.out.println("Could not read result sent by server");
				result = new Result(null, -1);
			}
				
		} catch(IOException e) {
			System.out.println("Could not connect and send calculation info to server");
			result = new Result(null, -1);
			// closed in finally block
		} finally {
			try {
				if (out != null) { out.close(); }
				if (inob != null) { inob.close(); }
				if (socket != null) { socket.close(); }
 			} catch (IOException e) {
				// nothing to do if can't close socket.
			}
		}

	}
	
}