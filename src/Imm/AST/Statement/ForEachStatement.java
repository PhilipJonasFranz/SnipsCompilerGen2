package Imm.AST.Statement;

import java.util.List;

import CGen.LabelGen;
import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Expression.ArraySelect;
import Imm.AST.Expression.Atom;
import Imm.AST.Expression.Deref;
import Imm.AST.Expression.Expression;
import Imm.AST.Expression.IDRef;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import Par.Token.TokenType;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class ForEachStatement extends CompoundStatement {

			/* --- FIELDS --- */
	/** The declaration of the iterator. */
	public Declaration iterator;
	
	public Declaration counter;
	
	public IDRef ref;
	
	public Expression shadowRef;
	
	public Expression range;
	
	/** ArraySelect used when data is retrieved from an array */
	public ArraySelect select;
	
	/** Dereference used when data is retrieved from a pointer */
	public Deref deref;
	
	
			/* --- CONSTRUCTORS --- */
	public ForEachStatement(Declaration iterator, Expression shadowRef, Expression range, List<Statement> body, Source source) {
		super(body, source);
		this.iterator = iterator;
		this.shadowRef = shadowRef;
		
		this.range = range;
		
		String name = "__it_cnt" + LabelGen.getSafeNum();
		
		/* Initialize internal Counter and reference to it */
		counter = new Declaration(new NamespacePath(name), new INT("0"), new Atom(new INT("0"), new Token(TokenType.INTLIT, this.shadowRef.getSource(), "0"), this.shadowRef.getSource()), MODIFIER.SHARED, this.shadowRef.getSource());
		ref = new IDRef(new NamespacePath(name), this.shadowRef.getSource());
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "ForEach");
		if (rec) {
			this.iterator.print(d + this.printDepthStep, rec);
			this.shadowRef.print(d + this.printDepthStep, rec);
			
			for (Statement s : this.body) 
				s.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkForEachStatement(this);
	}
	
} 