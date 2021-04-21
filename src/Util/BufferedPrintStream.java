package Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class BufferedPrintStream {

	private PrintStream outs;
	
	private List<String> printed = new ArrayList();
	
	private String internalBuffer = "";
	
	public BufferedPrintStream(PrintStream outs) {
		this.outs = outs;
	}
	
	public void println(String s) {
		this.internalBuffer += s;
		this.printed.add(this.internalBuffer);
		
		outs.println(s);
		this.internalBuffer = "";
	}
	
	public void println() {
		this.printed.add(this.internalBuffer);
		
		outs.println();
		this.internalBuffer = "";
	}
	
	public void print(String s) {
		this.internalBuffer += s;
		outs.print(s);
	}
	
	public void flush() {
		this.printed.add(this.internalBuffer);
		outs.println(this.internalBuffer);
		this.internalBuffer = "";
	}
	
	public List<String> getContents() {
		return this.printed;
	}
	
}
