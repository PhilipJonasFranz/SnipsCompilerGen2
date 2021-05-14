package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Imm.AST.SyntaxElement;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Par.Token;
import Tools.ASTNodeVisitor;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Comment extends Statement {

			/* ---< FIELDS >--- */
	public String comment;
	
	
			/* ---< CONSTRUCTORS >--- */
	public Comment(Token comment, Source source) {
		super(source);
		this.comment = comment.spelling();
	}
	
	public Comment(String comment, Source source) {
		super(source);
		this.comment = comment;
	}
	
	
			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {}

	public TYPE check(ContextChecker ctx) {
		return null;
	}
	
	public Statement opt(ASTOptimizer opt) {
		return this;
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		return result;
	}
	
	public void setContext(List<TYPE> context) {}

	public Statement clone() {
		Comment com = new Comment(this.comment, this.getSource().clone());
		com.copyDirectivesFrom(this);
		return com;
	}

	public List<String> codePrint(int d) {
		return new ArrayList();
	}

} 
