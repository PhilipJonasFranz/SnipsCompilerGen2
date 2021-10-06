package Exc;

import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;

/**
 * Thrown when internal modules are missing or when the user input leads to an invalid combination
 * of components. Latteral should mostly not occur since invalid combinations are filtered by the context 
 * checker.
 */
public class CGEN_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	private String message;
	
	private Source location;
	
	private Object [] format;
	
	public CGEN_EXC(Source source, String message, Object...format) {
		this.location = source;
		this.message = message;
		this.format = format;
		
		AsNBody.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public CGEN_EXC(String message, Object...format) {
		this.message = message;
		this.format = format;
		
		AsNBody.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public String getExcFieldName() {
		return Util.Util.getExceptionFieldName(this.message);
	}
	
	public String getMessage() {
		return String.format(message, this.format) + ((this.location != null)? ", " + this.location.getSourceMarker() : "");
	}
	
} 
