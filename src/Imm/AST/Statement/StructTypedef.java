package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public String structName;
	
	public STRUCT struct;
	
	public List<Declaration> declarations;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructTypedef(Token id, List<Declaration> declarations, Source source) {
		super(source);
		this.structName = id.spelling;
		this.declarations = declarations;
		
		/* Call with reference on itself, struct type will be built when context checking */
		this.struct = new STRUCT(this);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Struct Typedef <" + this.structName + ">");
		if (rec) {
			for (Declaration dec : this.declarations) {
				dec.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		for (Declaration dec : this.declarations) dec.setContext(context);
	}

	public void releaseContext() {
		for (Declaration dec : this.declarations) dec.releaseContext();
	}
	
}
