package Imm.AST;

import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* --- FIELDS --- */
	public TYPE returnType;
	
	public String functionName;
	
	public List<Declaration> parameters;
	
			/* --- CONSTRUCTORS --- */
	public Function(TYPE returnType, Token functionId, List<Declaration> parameters, List<Statement> statements, Source source) {
		super(statements, source);
		this.returnType = returnType;
		this.functionName = functionId.spelling;
		this.parameters = parameters;
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		for (Directive dir : this.directives) dir.print(d, rec);
		System.out.print(this.pad(d) + "<" + this.returnType.typeString() + "> " + this.functionName + "(");
		for (int i = 0; i < this.parameters.size(); i++) {
			Declaration dec = parameters.get(i);
			System.out.print("<" + dec.type.typeString() + "> " + dec.fieldName);
			if (i < this.parameters.size() - 1) System.out.print(", ");
		}
		System.out.println(")");
		if (rec) {
			for (Statement s : body) {
				s.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkFunction(this);
	}
	
}
