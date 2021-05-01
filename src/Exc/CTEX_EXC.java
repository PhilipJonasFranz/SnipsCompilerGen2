package Exc;

import Ctx.ContextChecker;
import Snips.CompilerDriver;
import Util.Source;
import Util.Util;
import Util.Logging.LogPoint;
import Util.Logging.Message;

/**
 * Thrown when the user input led to an illegal combination of components.
 */
public class CTEX_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	private String message;
	
	private Source location;
	
	private Object [] format;
	
	public CTEX_EXC(String message, Object...format) {
		this.location = CompilerDriver.stackTrace.peek().getSource();
		this.message = message;
		this.format = format;
		
		ContextChecker.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
		
		Util.buildStackTrace(this.location.sourceFile);
	}
	
	public CTEX_EXC(Source source, String message, Object...format) {
		this.location = source;
		this.message = message;
		this.format = format;
		
		ContextChecker.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
		
		Util.buildStackTrace(this.location.sourceFile);
	}
	
	public String getExcFieldName() {
		return Util.getExceptionFieldName(this.message);
	}
	
	public String getMessage() {
		return String.format(message, format) + ", " + this.location.getSourceMarker();
	}
	
} 
