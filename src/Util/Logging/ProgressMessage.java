package Util.Logging;

import Snips.CompilerDriver;
import Util.Logging.Message.Type;

/** A message that has a status header, a message type and a capsuled message string. */
public class ProgressMessage {
	
	public final String ANSI_RESET  = "\u001B[0m";
	public final String ANSI_BLACK  = "\u001B[30m";
	public final String ANSI_RED    = "\u001B[31m";
	public final String ANSI_GREEN  = "\u001B[32m";
	public final String ANSI_YELLOW = "\u001B[33m";
	public final String ANSI_BLUE   = "\u001B[34m";
	public final String ANSI_PURPLE = "\u001B[35m";
	public final String ANSI_CYAN   = "\u001B[36m";
	public final String ANSI_WHITE  = "\u001B[37m";
	
	public Type messageType;
	
	protected String message;
	
	protected int maxProgress;
	
	public ProgressMessage(String message, int maxProgress, Type type) {
		this.message = message;
		this.messageType = type;
		this.maxProgress = maxProgress;
		if (!CompilerDriver.silenced) {
			if (type != Type.WARN || !CompilerDriver.disableWarnings) {
				System.out.print(this.getMessage());
			}
		}
	}
	
	public ProgressMessage(String message, Type type, boolean buffered) {
		this.message = message;
		this.messageType = type;
		if (!CompilerDriver.silenced && !buffered) System.out.print(this.getMessage());
	}
	
	public double last = 0;
	
	protected int printed = 0;
	
	public void incProgress(double progress) {
		if (!CompilerDriver.silenced) {
			while (this.maxProgress * progress > printed) {
				System.out.print(".");
				printed++;
			}
			
			if (progress == 1) {
				System.out.println("DONE!");
			}
		}
	}
	
	public void incProgressSingle() {
		if (!CompilerDriver.silenced) {
			System.out.print(".");
			printed++;
		}
	}
	
	public void abort() {
		if (!CompilerDriver.silenced) {
			while (printed < this.maxProgress) {
				System.out.print(".");
				printed++;
			}
			System.out.println("ERROR!");
		}
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