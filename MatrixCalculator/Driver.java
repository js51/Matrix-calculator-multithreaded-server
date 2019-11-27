import java.util.ArrayList;

/**
 * 'Simulates' multiple concurrent client connections on one PC 
 * by creating instances of MatrixClient and calling their 
 * 'calculate' methods.
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class Driver {
	
	public static void main(String[] args) {
		
		// Run tests here
		basicTest(10, Operation.cyclicv1, 500);
		
	}
	
	// Performs a set number of calculations as one client, using the given
	// partitioning method and matrix size. Does not request that the resulting matrix
	// be returned.
	public static void basicTest(int numTests, Operation operation, int matrixSize) {

		long[] times = new long[numTests];
		
		// Perform the same calculation the given number of times, recording time taken.
		for (int i = 0; i < numTests; i++) {
			final long startTime = System.currentTimeMillis();
			MatrixClient mc = new MatrixClient();
			Result result = mc.calculate(operation, matrixSize, false);
			final long duration = System.currentTimeMillis() - startTime;
			System.out.println("Took " + duration + " milliseconds\n");
			times[i] = duration;
		}			
		
		// Calculate average wait time out of the (numTests) tests.
		double average = 0;
		for (int i = 0; i<times.length; i++) {
			average += times[i];
		}	
		average = average / times.length;
		
		System.out.println("Average time to complete: " + average);
	}
	
	// Connects the given number of clients (numClients) to the server to perform that many
	// calculations. Uses the given partitioning method (operation) and matrix size. 
	// Does not request that the resulting matrix be returned.
	public static void testMultipleClients(int numClients, Operation operation, int matrixSize) {
		
		// Internal class for representing a client.
		// Each one will create a new MatrixClient and call calculate once.
		class VirtualClient extends Thread {
			public long timeTaken = 0;
			public int clientID = 0;
			public VirtualClient(int clientID) {
				this.clientID = clientID;
				start();
			}
			public void run() {
				final long startTime = System.currentTimeMillis();
				System.out.println("Starting Client #" + clientID);
				MatrixClient mc = new MatrixClient();
				Result result = mc.calculate(operation, matrixSize, false);
				final long duration = System.currentTimeMillis() - startTime;
				System.out.println(duration);
				timeTaken = duration;
			}
		}
		
		ArrayList<VirtualClient> clients = new ArrayList<>();
		
		// Create and start client threads.
		for (int i=0; i<numClients; i++) {
			VirtualClient c = new VirtualClient(i+1);
			clients.add(c);
		}
		
		// Wait for clients to be served
		try {
			for (int i=0; i<clients.size(); i++) {
				clients.get(i).join();
			}
		} catch (InterruptedException e) { }
		
		// Calculate average wait time for clients.
		double average = 0;
		for (int i=0; i<clients.size(); i++) {
			average += clients.get(i).timeTaken;
		}
		average = average / clients.size();
		
		System.out.println("\n" + "With " + numClients + " clients, clients were served in " + average + " miliseconds on average");
	}
}