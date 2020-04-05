package Exc;

import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.Message;

/**
 * Thrown when the user input led to an illegal combination of components.
 */
public class CTX_EXCEPTION extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String message;
	
	Source location;
	
	public CTX_EXCEPTION(Source source, String message) {
		this.location = source;
		this.message = message;
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		return message + ", " + this.location.getSourceMarker();
	}
	
}
