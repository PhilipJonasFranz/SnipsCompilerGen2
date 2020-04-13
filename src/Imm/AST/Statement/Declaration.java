package Imm.AST.Statement;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Expression.Expression;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Declaration extends Statement {

			/* --- FIELDS --- */
	public String fieldName;
		
	public TYPE type;
	
	public Expression value;
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public Declaration(Token id, TYPE type, Source source) {
		super(source);
		this.fieldName = id.spelling;
		this.type = type;
	}
	
	public Declaration(Token id, TYPE type, Expression value, Source source) {
		super(source);
		this.fieldName = id.spelling;
		this.type = type;
		this.value = value;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Declaration <" + this.type.typeString() + "> " + this.fieldName);
		if (rec && this.value != null) {
			this.value.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkDeclaration(this);
	}
	
}
