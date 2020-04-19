package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXCEPTION;
import Imm.AST.Directive.Directive;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.Statement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Par.Token;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* --- FIELDS --- */
	public ProvisoManager manager;
	
	private TYPE returnType;
	
	public String functionName;
	
	public List<Declaration> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	public Function(TYPE returnType, Token functionId, List<TYPE> proviso, List<Declaration> parameters, List<Statement> statements, Source source) {
		super(statements, source);
		this.returnType = returnType;
		this.functionName = functionId.spelling;
		this.parameters = parameters;
		
		this.manager = new ProvisoManager(this.getSource(), proviso);
		
		if (proviso.isEmpty()) {
			/* Add default mapping */
			this.manager.addProvisoMapping(null, new ArrayList());
		}
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		for (Directive dir : this.directives) dir.print(d, rec);
		System.out.print(this.pad(d) + "<" + this.returnType.typeString() + "> " + this.functionName + "(");
		for (int i = 0; i < this.parameters.size(); i++) {
			Declaration dec = parameters.get(i);
			System.out.print("<" + dec.getType().typeString() + "> " + dec.fieldName);
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

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		//System.out.println("Applied Context: " + this.getClass().getName());
		
		/* Apply context to existing proviso types */
		this.manager.setContext(context);
		
		/* Apply to parameters */
		for (Declaration d : this.parameters) {
			d.setContext(this.manager.provisosTypes);
		}
		
		/* Apply to return type */
		ProvisoManager.setContext(this.manager.provisosTypes, this.returnType);
		
		/* Apply to body */
		for (Statement s : this.body) {
			s.setContext(this.manager.provisosTypes);
		}
		
		if (!this.manager.containsMapping(context)) {
			/* Save this context mapping, save copy of return type */
			this.manager.addProvisoMapping(this.getReturnType().clone(), context);
		}
	}

	public void releaseContext() {
		this.manager.releaseContext();
		
		for (Declaration d : this.parameters) {
			d.releaseContext();
		}
		
		for (Statement s : this.body) {
			s.releaseContext();
		}
		
		if (this.returnType instanceof PROVISO) {
			PROVISO ret = (PROVISO) this.returnType;
			ret.releaseContext();
		}
	}
	
	/** 
	 * Return the current context, or the actual type.
	 */
	public TYPE getReturnType() {
		if (this.returnType instanceof PROVISO) {
			PROVISO p = (PROVISO) this.returnType;
			if (p.hasContext()) return p.getContext();
			else return p;
		}
		else return this.returnType;
	}
	
	public void setReturnType(TYPE type) {
		this.returnType = type;
	}
	
}
