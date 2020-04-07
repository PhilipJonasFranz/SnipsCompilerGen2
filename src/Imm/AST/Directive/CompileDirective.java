package Imm.AST.Directive;

import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class CompileDirective extends Directive {

			/* --- NESTED --- */
	public enum COMP_DIR {
		OPERATOR, UNROLL, LIBARY;
	}
	
	
			/* --- FIELDS --- */
	public COMP_DIR compDir;
	
	
			/* --- CONSTRUCTORS --- */
	public CompileDirective(COMP_DIR directive, Source source) {
		super(source);
		this.compDir = directive;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "#" + this.compDir.toString().toLowerCase());
	}

}
