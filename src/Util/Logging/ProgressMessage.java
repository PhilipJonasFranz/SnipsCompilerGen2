package Util.Logging;

import Snips.CompilerDriver;
import Util.Logging.LogPoint.Type;

/** A message that has a status header, a message type and a capsuled message string. */
public class ProgressMessage {
	
	public Type messageType;
	
	protected String message;
	
	protected int maxProgress;
	
	private boolean aborted = false;
	
	public ProgressMessage(String message, int maxProgress, Type type) {
		this.message = message;
		this.messageType = type;
		this.maxProgress = maxProgress;
		if (!CompilerDriver.silenced) {
			if (type != Type.WARN || !CompilerDriver.disableWarnings) {
				CompilerDriver.outs.print(this.getMessage());
			}
		}
	}

	/**
	 * Set to true when a progress value has been seen
	 * with the value 1.
	 */
	public boolean isDone = false;
	
	/**
	 * How many dots have been printed out already.
	 */
	protected int printed = 0;
	
	/**
	 * Sets the progress to given value. Range: [0-1].
	 * Also prints out progress dots (...) based on the value.
	 * When calling this function multiple times, the passed
	 * param values should be ascending, so x0 <= x1 <= x2.
	 * If this is not the case, no dots will be printed, since a
	 * previous higher value already printed the dots.
	 * 
	 * Prints a 'DONE!' message if the given value is 1.
	 */
	public void incProgress(double progress) {
		if (!CompilerDriver.silenced) {
			while (this.maxProgress * progress > printed && !isDone) {
				CompilerDriver.outs.print(".");
				printed++;
			}
			
			if (progress == 1 && !isDone) {
				CompilerDriver.outs.println("DONE!");
				isDone = true;
			}
		}
	}
	
	/**
	 * Equivalent to {@linkplain #incProgress(1)}.
	 */
	public void finish() {
		this.incProgress(1);
	}

	/**
	 * Prints out all remaining dots and a 'ERROR!' message at the end.
	 */
	public void abort() {
		if (!aborted) {
			aborted = true;
			if (!CompilerDriver.silenced) {
				while (printed < this.maxProgress) {
					CompilerDriver.outs.print(".");
					printed++;
				}
				
				CompilerDriver.outs.println("ERROR!");
			}
		}
	}
	
	/** Flush the buffered message to the console. */
	public void flush() {
		if (!CompilerDriver.silenced) CompilerDriver.outs.println(this.getMessage());
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
