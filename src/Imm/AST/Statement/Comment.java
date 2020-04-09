package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Comment extends Statement {

			/* --- FIELDS --- */
	public String comment;
	
	
			/* --- CONSTRUCTORS --- */
	public Comment(Token comment, Source source) {
		super(source);
		this.comment = comment.spelling;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Comment: " + this.comment);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return null;
	}
	
}
