package Imm.AST.Statement;

import java.util.List;

import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class BreakStatement extends Statement {

			/* --- FIELDS --- */
	/** The loop this break statements breaks out of. */
	public CompoundStatement superLoop;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public BreakStatement(Source source) {
		super(source);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Break");
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

} 
