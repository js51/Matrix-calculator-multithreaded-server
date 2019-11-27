import java.io.Serializable;
import java.util.jar.*;

/**
 * Represents the result of a matrix operation
 *
 * @author Joshua Stevenson (ID: 386572)
 */
class Result implements Serializable{
	int errorCode;		
	double[][] answer;
	
	public Result(double[][] ans, int err) {
		this.errorCode = err;
		this.answer = ans;
	}
}

