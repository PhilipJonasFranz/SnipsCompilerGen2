package Exc;

import Imm.AsN.AsNBody;
import Snips.CompilerDriver;
import Util.Source;
import Util.Logging.LogPoint;
import Util.Logging.Message;

public class OPT0_EXC extends Exception {

	private static final long serialVersionUID = 765217464625890214L;

	String message;
	
	Source location;
	
	Object [] format;
	
	public OPT0_EXC(Source source, String message, Object...format) {
		this.location = source;
		this.message = message;
		this.format = format;
		
		AsNBody.progress.abort();
		CompilerDriver.log.add(new Message(this.getMessage(), LogPoint.Type.FAIL));
	}
	
	public OPT0_EXC(String message, Object...format) {
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
