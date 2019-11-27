public class HelperMethods {
	
	// Prints a matrix (one row per line) 
	public static void printMatrix(double[][] matrix) {
		if (matrix != null) {
			String strMatrix = "[ ";
			for (int i=0; i<matrix.length; i++) {
				String row = "";
				for (int j=0; j<matrix[i].length; j++) {
					row = row + matrix[i][j] + " ";
				}
				if (i+1 == matrix.length) {
					strMatrix = strMatrix + row;
				} else {
					strMatrix = strMatrix + row + "\n";
				}
			}
			System.out.println(strMatrix + "]");
		} else {
			System.out.println("Matrix is null");
		}
	}

}