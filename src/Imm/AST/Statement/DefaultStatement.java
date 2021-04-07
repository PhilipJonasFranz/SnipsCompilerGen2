package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

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
		CompilerDriver.outs.println(Util.pad(d) + "Default");
		
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
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optDefaultStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		return result;
	}

	public Statement clone() {
		return new DefaultStatement(this.cloneBody(), this.getSource().clone());
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		String s = "default : {";
		code.add(Util.pad(d) + s);
		
		for (Statement s0 : this.body)
			code.addAll(s0.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}
	
} 
