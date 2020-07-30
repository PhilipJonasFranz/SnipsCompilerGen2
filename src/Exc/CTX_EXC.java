package Exc;

import Ctx.ContextChecker;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.Message;

/**
 * Thrown when the user input led to an illegal combination of components.
 */
public class CTX_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String message;
	
	Source location;
	
	public CTX_EXC(Source source, String message) {
		this.location = source;
		this.message = message;
		ContextChecker.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		return message + ", " + this.location.getSourceMarker();
	}
	
} 
