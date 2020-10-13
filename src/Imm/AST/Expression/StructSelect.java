package Imm.AST.Expression;

import java.util.List;

import Exc.CTX_EXC;
import Imm.TYPE.TYPE;
import Util.Source;

public class StructSelect extends Expression {

			/* --- FIELDS --- */
	public Expression selector;
	
	public boolean deref;
	
	public Expression selection;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructSelect(Expression selector, Expression selection, boolean deref, Source source) {
		super(source);
		this.selection = selection;
		this.selector = selector;
		this.deref = deref;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Struct" + ((this.deref)? "Pointer" : "") + "Select");
		if (rec) {
			this.selector.print(d + this.printDepthStep, rec);
			this.selection.print(d + this.printDepthStep, rec);
		}
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		this.selector.setContext(context);
		this.selection.setContext(context);
	}

} 
