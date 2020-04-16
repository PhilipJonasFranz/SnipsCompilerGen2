package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
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
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** A list that contains the combinations of types this function was templated with */
	public List<List<TYPE>> provisosCalls = new ArrayList();
	
	private TYPE returnType;
	
	public String functionName;
	
	public List<Declaration> parameters;
	
	
			/* --- CONSTRUCTORS --- */
	public Function(TYPE returnType, Token functionId, List<TYPE> proviso, List<Declaration> parameters, List<Statement> statements, Source source) {
		super(statements, source);
		this.returnType = returnType;
		this.functionName = functionId.spelling;
		this.provisosTypes = proviso;
		this.parameters = parameters;
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
		
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			if (!(pro instanceof PROVISO)) {
				throw new CTX_EXCEPTION(this.getSource(), "Provided Type " + pro.typeString() + " is not a proviso type.");
			}
			
			PROVISO pro0 = (PROVISO) pro;
			//System.out.println("Applied " + context.get(i).typeString() + " to proviso " + pro0.typeString());
			pro0.setContext(context.get(i));
			//System.out.println("New proviso: " + pro0.typeString());
		}
		
		if (this.returnType instanceof PROVISO) {
			PROVISO ret = (PROVISO) this.returnType;
			for (int i = 0; i < this.provisosTypes.size(); i++) {
				TYPE pro = this.provisosTypes.get(i);
				
				if (pro.isEqual(ret)) {
					//System.out.println("Applied " + context.get(i).typeString() + " to return proviso " + ret.typeString());
					ret.setContext(context.get(i));
					//System.out.println("New return proviso: " + ret.typeString());
				}
			}
		}
		
		for (Statement s : this.body) {
			s.setContext(this.provisosTypes);
		}
	}

	public void releaseContext() {
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			PROVISO pro0 = (PROVISO) this.provisosTypes.get(i);
			pro0.releaseContext();
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
