package Util.Logging;

import Snips.CompilerDriver;

/** This message prints itself without message type header. */
public class SimpleMessage extends Message {
	
	public SimpleMessage(String message) {
		super(message, Message.Type.INFO);
		if (!CompilerDriver.silenced) System.out.println(this.getMessage());
	}
	
	public SimpleMessage(String message, boolean buffered) {
		super(message, Message.Type.INFO, buffered);
		if (!CompilerDriver.silenced && !buffered) System.out.println(this.getMessage());
	}
	
	public void flush() {
		if (!CompilerDriver.silenced) System.out.println(this.getMessage());
	}
	
	public String getMessage() {
		return this.message;
	}
	
}
