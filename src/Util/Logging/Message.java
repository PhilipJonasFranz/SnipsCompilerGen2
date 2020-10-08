package Util.Logging;

import Snips.CompilerDriver;
import Util.Logging.LogPoint.Type;

/** A message that has a status header, a message type and a capsuled message string. */
public class Message {
		
	public Type messageType;
	
	protected String message;
	
	public Message(String message, Type type) {
		this.message = message;
		this.messageType = type;
		if (!CompilerDriver.silenced) {
			if (type != Type.WARN || !CompilerDriver.disableWarnings) {
				System.out.println(this.getMessage());
			}
		}
	}
	
	public Message(String message, Type type, boolean buffered) {
		this.message = message;
		this.messageType = type;
		if (!CompilerDriver.silenced && !buffered) System.out.println(this.getMessage());
	}
	
	/** Flush the buffered message to the console. */
	public void flush() {
		if (!CompilerDriver.silenced) System.out.println(this.getMessage());
	}
	
	public String getMessage() {
		return this.getTypeString() + this.message;
	}
	
	private String getTypeString() {
		if (CompilerDriver.useTerminalColors)
			return "[" + LogPoint.getEscapeCodeFor(this.messageType) + LogPoint.typeToString(this.messageType) + LogPoint.ColorCodes.ANSI_RESET + "] : ";
		else
			return "[" + LogPoint.typeToString(this.messageType) + "] : ";
	}
	
} 
