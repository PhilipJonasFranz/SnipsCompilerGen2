package Exc;

import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.Message;

/**
 * Thrown when internal modules are missing or when the user input leads to an invalid combination
 * of components. Latteral should mostly not occur since invalid combinations are filtered by the context 
 * checker.
 */
public class CGEN_EXCEPTION extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String message;
	
	Source location;
	
	public CGEN_EXCEPTION(Source source, String message) {
		this.location = source;
		this.message = message;
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public CGEN_EXCEPTION(String message) {
		this.message = message;
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		return message + ((this.location != null)? ", " + this.location.getSourceMarker() : "");
	}
	
}
