package Exc;

import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Logging.LogPoint;
import Util.Logging.Message;

import java.io.Serial;

/**
 * Thrown when a Snips intern component fails unexpectedly, or if a component fails and it is
 * not related to the user input.
 */
public class SNIPS_EXC extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 765217464625890214L;

	private String message;
	
	private Object [] format;
	
	public SNIPS_EXC() {
		this.message = "An error has occurred.";
		
		if (AsNBody.progress != null) AsNBody.progress.abort();
		
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public SNIPS_EXC(String errorMessage, Object...format) {
		this.message = errorMessage;
		this.format = format;
		
		if (AsNBody.progress != null) AsNBody.progress.abort();
		
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}

	public String getExcFieldName() {
		return Util.Util.getExceptionFieldName(this.message);
	}
	
	public String getMessage() {
		return String.format(this.message, format);
	}
	
	public String getDirectMessage() {
		return this.message;
	}
	
} 
