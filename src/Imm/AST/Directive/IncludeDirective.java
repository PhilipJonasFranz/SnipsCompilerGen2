package Imm.AST.Directive;

import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class IncludeDirective extends Directive {

			/* --- FIELDS --- */
	public String file;
	
	
			/* --- CONSTRUCTORS --- */
	public IncludeDirective(String file, Source source) {
		super(source);
		this.file = file;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "#include<" + this.file + ">");
	}

}
