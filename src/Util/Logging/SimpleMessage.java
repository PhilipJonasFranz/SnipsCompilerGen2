package Util.Logging;

import Snips.CompilerDriver;

/** This message prints itself without message type header. */
public class SimpleMessage extends Message {
	
	public SimpleMessage(String message) {
		super(message, LogPoint.Type.INFO);
		if (!CompilerDriver.silenced) CompilerDriver.outs.println(this.getMessage());
	}
	
	public SimpleMessage(String message, boolean buffered) {
		super(message, LogPoint.Type.INFO, buffered);
		if (!CompilerDriver.silenced && !buffered) CompilerDriver.outs.println(this.getMessage());
	}
	
	public void flush() {
		if (!CompilerDriver.silenced) CompilerDriver.outs.println(this.getMessage());
	}
	
	public String getMessage() {
		return this.message;
	}
	
} 
