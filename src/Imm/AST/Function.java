package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* --- FIELDS --- */
	public MODIFIER modifier = MODIFIER.SHARED;
	
	public ProvisoManager manager;
	
	private TYPE returnType;
	
	public NamespacePath path;
	
	public List<Declaration> parameters;
	
	public boolean signals;
	
	public List<TYPE> signalsTypes;
	
	/* Flags for lambda targeting */
	public Declaration lambdaDeclaration;
	
	public boolean isLambdaTarget = false;
	
	public boolean isLambdaHead = false;
	
	public boolean hasReturn = false;
	
	public ReturnStatement noReturn = null;
	
	
			/* --- CONSTRUCTORS --- */
	public Function(TYPE returnType, NamespacePath path, List<TYPE> proviso, List<Declaration> parameters, boolean signals, List<TYPE> signalsTypes, List<Statement> statements, MODIFIER modifier, Source source) {
		super(statements, source);
		this.returnType = returnType;
		this.path = path;
		this.parameters = parameters;
		
		this.signals = signals;
		this.signalsTypes = signalsTypes;
		
		this.manager = new ProvisoManager(this.getSource(), proviso);
		
		this.modifier = modifier;
		
		if (path.build().equals("main")) {
			/* Add default mapping */
			this.manager.addProvisoMapping(null, new ArrayList());
		}
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "<" + this.returnType.typeString() + "> " + this.path.build() + "(");
		for (int i = 0; i < this.parameters.size(); i++) {
			Declaration dec = parameters.get(i);
			System.out.print("<" + dec.getRawType().typeString() + "> " + dec.path.build());
			if (i < this.parameters.size() - 1) System.out.print(", ");
		}
		System.out.print(")");
		
		if (!this.signalsTypes.isEmpty()) {
			System.out.print(" signals ");
			for (int i = 0; i < this.signalsTypes.size(); i++) {
				System.out.print(this.signalsTypes.get(i).typeString());
				if (i < this.signalsTypes.size() - 1) System.out.print(", ");
			}
		}
		
		System.out.println(" " + this.toString().split("@") [1]);
		if (rec) {
			for (Statement s : body) {
				s.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkFunction(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		/* Apply context to existing proviso types */
		this.manager.setContext(context);
		
		/* Apply to return type */
		ProvisoUtil.mapNTo1(this.returnType, this.manager.provisosTypes);
		
		/* Apply to parameters */
		for (Declaration d : this.parameters) {
			d.setContext(this.manager.provisosTypes);
		}
		
		/* Add to mapping pool */
		if (!this.manager.containsMapping(context)) {
			/* Save this context mapping, save copy of return type */
			this.manager.addProvisoMapping(this.getReturnType().clone(), context);
		}
		
		/* Apply to body */
		for (Statement s : this.body) {
			s.setContext(this.manager.provisosTypes);
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
