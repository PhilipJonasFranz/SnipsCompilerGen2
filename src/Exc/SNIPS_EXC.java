package Exc;

import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Logging.Message;

/**
 * Thrown when a Snips intern component fails unexpectedly, or if a component fails and it is
 * not related to the user input.
 */
public class SNIPS_EXC extends RuntimeException {

	private static final long serialVersionUID = 765217464625890214L;

	String errorMessage;
	
	Object [] format;
	
	public SNIPS_EXC(String errorMessage, Object...format) {
		this.errorMessage = errorMessage;
		this.format = format;
		
		if (AsNBody.progress != null) AsNBody.progress.abort();
		
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		return String.format(this.errorMessage, format);
	}
	
} 
