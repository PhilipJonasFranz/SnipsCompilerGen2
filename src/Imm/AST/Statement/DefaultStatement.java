package Imm.AST.Statement;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Imm.TYPE.TYPE;
import Snips.CompilerDriver;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class DefaultStatement extends CompoundStatement {

			/* ---< CONSTRUCTORS >--- */
	public DefaultStatement(List<Statement> body, Source source) {
		super(body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Default");
		
		if (rec) for (Statement s : this.body) 
			s.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkDefaultStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}

	public Statement clone() {
		return new DefaultStatement(this.cloneBody(), this.getSource().clone());
	}
	
} 
