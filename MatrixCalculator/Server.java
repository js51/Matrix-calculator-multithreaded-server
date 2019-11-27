import java.net.*;
import java.io.*;
import java.util.ArrayList;

/**
 * Server for performing matrix operations
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class MatrixServer {
	
	public static int PORT_NUMBER;
	public static int NUM_WORKERS;
	public static int[] NUM_BLOCKS;

	/**
	 * Starts the server.
	 * @param args 0: port number, 1: number of workers.
	 * @return Nothing.
	 */ 
	public static void main(String[] args) throws IOException {
		
		PORT_NUMBER = Integer.parseInt(args[0]);
		NUM_WORKERS = Integer.parseInt(args[1]);
		
		// Calculating most balanced factors of NUM_WORKERS
		// Only used if the blockv1 operation method is used,
		// but still most efficient to perform this here.
		int[] bestFactors = {1, NUM_WORKERS};
		int largestDifference = NUM_WORKERS - 1;
		for (int f = 2; f < NUM_WORKERS; f++) {
			if (NUM_WORKERS%f == 0) { // f divides number of workers
				int diff = Math.abs(f - NUM_WORKERS/f);
				if (diff < largestDifference) {
					largestDifference = diff;
					bestFactors[0] = f;
					bestFactors[1] = NUM_WORKERS/f;
				}
			}
		}
		NUM_BLOCKS = bestFactors; // most balanced factors of NUM_WORKERS	
		
		// Open the server socket
		ServerSocket server = new ServerSocket(PORT_NUMBER);
		System.out.print("Server opened on port " + PORT_NUMBER);
		System.out.println(" with " + NUM_WORKERS + " available worker threads.");
		
		// Listen for client connections
		int clientConnections = 0;
		try {
			while(true) {
				Socket socket = server.accept();
				try {
					// Create a thread to handle this client request
					new ClientHandlerThread(socket, clientConnections, NUM_WORKERS);
				} catch(IOException e) {
					System.out.println("Connection with client #" + (clientConnections + 1) + " failed");
					socket.close();
				} finally {
					clientConnections++;
				}
			}
		} catch (IOException e) {
			server.close();
		} finally {
			server.close();
		}
	}
	
	// Generates a random (size x size) matrix.
	public static double[][] generateMatrix(int size) {
		double[][] matrix = new double[size][size];
		for (int i=0; i<size; i++) {
			for (int j=0; j<size; j++) {
				matrix[i][j] = 10 * Math.random();
			}
		}
		return matrix;
	}
	
	// Generates a (size x size) identity matrix (used for testing)
	public static double[][] generateIdentityMatrix(int size) {
		double[][] matrix = new double[size][size];
		for (int i=0; i<size; i++) {
			for (int j=0; j<size; j++) {
				if (i == j) {
					matrix[i][j] = 1;
				} else {
					matrix[i][j] = 0;
				}
			}
		}
		return matrix;
	}
	
}

/**
 * Server thread (handles one client connection)
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class ClientHandlerThread extends Thread {
		
	double[][] A;
	double[][] B;
	double[][] C;
	
	private Socket socket;
	private DataInputStream in;
	private ObjectOutputStream outob;
	
	private boolean returnMatrixResult;	// Whether or not to return the matrix in the 'Result'
	public int numWorkers;				// Number of workers to use for calculation			
	public Operation operation;			// Partitioning method
	
	private int clientNum;
	private Result result;
    
	public ClientHandlerThread(Socket s, int cNum, int nWorkers) throws IOException {
		socket = s;
		numWorkers = nWorkers;
		clientNum = cNum;
		start();
	}
	
	public void run() {
	
		ArrayList<WorkerThread> threads = new ArrayList<WorkerThread>(); // Workers
		
		System.out.println("Connected to client #" + clientNum);
		int size = 0;
		try {
			in = new DataInputStream(socket.getInputStream());
			size = in.readInt(); // Matrix size
			int opOrdinal = in.readInt(); // Operation 
			operation = Operation.values()[opOrdinal]; // Operation (converted back to enum)
			returnMatrixResult = in.readBoolean(); // Whether or not to return a matrix
		} catch (IOException e) {
			System.out.println("Could not read size and operation type from client #" + clientNum);
		} 
		
		// Generate matrices to multiply together (A and B) and empty matrix to store result (C)
		System.out.println("Generating matrices A and B of size " + size + " for Client #" + clientNum);
		A = MatrixServer.generateMatrix(size);
		B = MatrixServer.generateMatrix(size);
		C = new double[size][size];
				
		// Starting worker threads to perform calculation
		for (int w=0; w<numWorkers; w++) {
			try {
				WorkerThread t = new WorkerThread(this, w);
				threads.add(t); 
			} catch (IOException e) {
				System.out.println("Could not create worker thread #" + w);
				System.out.println("Result will not be correct, exiting...");
				System.exit(1);
			}
		}

		// Wait for workers to all finish, then send result back to the client
		try {
			for (int i=0; i<threads.size(); i++) {
				try {
					threads.get(i).join();
				} catch (InterruptedException e) {
					System.out.println("Could not join thread " + i);
				}
			}
			if (returnMatrixResult) {
				this.result = new Result(C, 0);
				System.out.println("Returning result:");
				HelperMethods.printMatrix(this.result.answer);
				System.out.println("Result returned with error code " + this.result.errorCode);
			} else {
				System.out.println("Not returning result (not requested)");
				this.result = new Result(null, 0);
			} 
			outob = new ObjectOutputStream(socket.getOutputStream());
			outob.writeObject(this.result);
			System.out.println("Done serving client #" + clientNum);
			
		} catch (IOException e) {
			System.out.println("Could not send result over Socket.");
			System.exit(1);
			
		} finally {
			try {
				in.close();
				outob.close();
				socket.close();
			} catch (IOException e2) { 
				// nothing to do if can't close socket.
			}
		}
	}
}


/**
 * Worker thread (computes one unit of work)
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class WorkerThread extends Thread {
	
	ClientHandlerThread parent;	// Reference to parent thread, so shared memory can be accessed
	int workerNum;				// Worker ID
	int size;					// Size of output matrix
	int numWorkers;				// Number of workers allocated

	public WorkerThread(ClientHandlerThread parentThread, int wNum) throws IOException {
		workerNum = wNum;
		parent = parentThread;
		size = parent.A.length;
		numWorkers = parent.numWorkers;
		start();
	}
	
	public void run() {
	
		int elementsToCalculate = elementNum(size - 1, size - 1);
		
		Operation operation = parent.operation; 
		
		// Determine which elements of C to calculate based on partitioning method requested.
		switch (operation) {
			case cyclicv1: 
				for (int e=workerNum; e<=elementsToCalculate; e+=numWorkers) {
					calculateElement(e);
				}
				break;
				
			case cyclicv2:
				for (int row=workerNum; row<size; row+=numWorkers) {
					for (int col=0; col<size; col++) {
						calculateElement(elementNum(row, col));
					}
				}
				break;
				
			case blockv1:
				int blockRows = MatrixServer.NUM_BLOCKS[0]; // Number of rows of 'blocks'
				int blockCols = MatrixServer.NUM_BLOCKS[1]; // Number of columns of 'blocks'
				
				// Estimate of size of each block
				int blockHeight = size/blockRows;
				int blockWidth = size/blockCols;
			
				// Deciding the column this worker will start calculating from
				int myStartBlockCol = workerNum % blockCols;
				int myStartCol = myStartBlockCol*blockWidth;
				
				// Deciding the row this worker will start calculating from
				int myStartBlockRow =  workerNum / blockCols;
				int myStartRow = myStartBlockRow*blockHeight;
				
				// Determining the last end column and row the worker will calculate
				// If there isn't enough space for another block at the end, extend this one
				// down or right to fill the matrix.
				int myEndRow = myStartRow + blockHeight;
				if (myStartBlockRow == blockRows - 1) {
					myEndRow = size;
				}
				int myEndCol = myStartCol + blockWidth;
				if (myStartBlockCol == blockCols - 1) {
					myEndCol = size;
				}
				
				System.out.println("Worker " + workerNum + " does rows: (" + myStartRow + "," + myEndRow + ") & cols: (" + myStartCol + "," + myEndCol + ")" );
				
				for (int row = myStartRow; row < myEndRow; row++) {
					for (int col = myStartCol; col < myEndCol; col++) {
						calculateElement(elementNum(row, col));
					}
				}
				break;
		}
	}
	
	// Calculates the given element and stores in C matrix.
	public void calculateElement(int e) {
		int i = posOfElement(e)[0];
		int j = posOfElement(e)[1];
		int cVal = 0;
		for (int k=0; k<size; k++) {
			cVal += parent.A[i][k] * parent.B[k][j];
		}
		parent.C[i][j] = cVal;
	}
	
	// Takes position of element in matrix and returns it's single-number location
	// (These run from left to right, then top to bottom.
	public int elementNum(int i, int j) { 
		return ((i)*size + j); 
	}
	
	// Gives the (i,j)-position of an element in a matrix.
	public int[] posOfElement(int n) {
		int[] pos = {(int)(n/size), (int)(n%size)};
		return pos;
	}
	
}
