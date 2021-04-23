package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.AST.SyntaxElement;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class WhileStatement extends ConditionalCompoundStatement {

			/* ---< FIELDS >--- */
	public int CURR_UNROLL_DEPTH = 0;
	
	
			/* ---< CONSTRUCTORS >--- */
	public WhileStatement(Expression condition, List<Statement> body, Source source) {
		super(condition, body, source);
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "While");
		
		if (rec) {
			this.condition.print(d + this.printDepthStep, rec);
		
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkWhileStatement(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public Statement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optWhileStatement(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		result.addAll(this.condition.visit(visitor));
		for (Statement s : this.body)
			result.addAll(s.visit(visitor));
		
		return result;
	}

	public WhileStatement clone() {
		WhileStatement w = new WhileStatement((Expression) this.condition.clone(), this.cloneBody(), this.getSource().clone());
		w.copyDirectivesFrom(this);
		return w;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		code.add(Util.pad(d) + "while (" + this.condition.codePrint() + ") {");
		
		for (Statement s : this.body)
			code.addAll(s.codePrint(d + this.printDepthStep));
		
		code.add(Util.pad(d) + "}");
		return code;
	}
	
} 
