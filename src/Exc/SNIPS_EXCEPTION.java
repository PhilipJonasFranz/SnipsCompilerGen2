package Exc;

import Snips.CompilerDriver;
import Util.Logging.Message;

public class SNIPS_EXCEPTION extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String errorMessage;
	
	public SNIPS_EXCEPTION(String errorMessage) {
		this.errorMessage = errorMessage;
		CompilerDriver.log.add(new Message(this.getMessage(), Message.Type.FAIL));
	}
	
	public String getMessage() {
		return this.errorMessage;
	}
	
}
