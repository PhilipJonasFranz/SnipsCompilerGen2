package Util.Logging;

import Snips.CompilerDriver;

/** A message that has a status header, a message type and a capsuled message string. */
public class Message {
	
	public final String ANSI_RESET  = "\u001B[0m";
	public final String ANSI_BLACK  = "\u001B[30m";
	public final String ANSI_RED    = "\u001B[31m";
	public final String ANSI_GREEN  = "\u001B[32m";
	public final String ANSI_YELLOW = "\u001B[33m";
	public final String ANSI_BLUE   = "\u001B[34m";
	public final String ANSI_PURPLE = "\u001B[35m";
	public final String ANSI_CYAN   = "\u001B[36m";
	public final String ANSI_WHITE  = "\u001B[37m";
	
	public enum Type {
		INFO, WARN, FAIL;
	}
	
	public Type messageType;
	
	protected String message;
	
	public Message(String message, Type type) {
		this.message = message;
		this.messageType = type;
		if (!CompilerDriver.silenced) System.out.println(this.getMessage());
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
			return "[" + ((this.messageType == Type.INFO)? this.ANSI_GREEN + "Info" + this.ANSI_RESET : (this.messageType == Type.WARN)? this.ANSI_YELLOW + "Warn" + this.ANSI_RESET : this.ANSI_RED + "Fail" + this.ANSI_RESET) + "] : ";
		else
			return "[" + ((this.messageType == Type.INFO)? "Info" : (this.messageType == Type.WARN)? "Warn" : "Fail") + "] : ";	
	}
	
}
