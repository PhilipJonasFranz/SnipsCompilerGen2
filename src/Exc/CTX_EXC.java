package Exc;

import Ctx.ContextChecker;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;

/**
 * Thrown when the user input led to an illegal combination of components.
 */
public class CTX_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String message;
	
	Source location;
	
	Object [] format;
	
	public CTX_EXC(Source source, String message, Object...format) {
		this.location = source;
		this.message = message;
		this.format = format;
		
		ContextChecker.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public String getExcFieldName() {
		return Util.Util.getExceptionFieldName(this.message);
	}
	
	public String getMessage() {
		return String.format(message, format) + ", " + this.location.getSourceMarker();
	}
	
} 
