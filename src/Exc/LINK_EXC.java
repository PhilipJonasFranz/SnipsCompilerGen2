package Exc;

import Ctx.ContextChecker;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.Message;

/**
 * Thrown when the user input led to an illegal combination of components.
 */
public class LINK_EXC extends Exception {

	private static final long serialVersionUID = 5791314197219757362L;

	private String message;
	
	private Object [] format;
	
	public LINK_EXC(String message, Object...format) {
		this.message = message;
		this.format = format;
		
		ContextChecker.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public String getExcFieldName() {
		return Util.Util.getExceptionFieldName(this.message);
	}
	
	public String getMessage() {
		return String.format(message, format);
	}
	
} 
