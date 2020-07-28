package Imm.AST;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import Ctx.ContextChecker;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Util.NamespacePath;
import Util.Pair;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* --- FIELDS --- */
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** A list that contains the combinations of types this function was templated with */
	public List<Pair<String, Pair<TYPE, List<TYPE>>>> provisosCalls = new ArrayList();
	
	public MODIFIER modifier = MODIFIER.SHARED;
	
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
		
		this.modifier = modifier;
		
		this.provisosTypes = proviso;
		
		if (path.build().equals("main")) {
			/* Add default mapping */
			this.addProvisoMapping(null, new ArrayList());
		}
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.print(this.pad(d) + "<" + this.returnType.typeString() + "> " + this.path.build());
		
		if (!this.provisosTypes.isEmpty()) {
			System.out.print("<");
			for (int i = 0; i < this.provisosTypes.size(); i++) {
				System.out.print(this.provisosTypes.get(i).typeString());
				if (i < this.provisosTypes.size() - 1) System.out.print(", ");
			}
			System.out.print(">");
		}
		
		System.out.print("(");
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

	/**
	 * Set given context to the function, including parameters, return type and body.
	 * Check if the given mapping already existed in the mapping pool, if not create a 
	 * new proviso-free mapping and store it in the proviso calls.
	 */
	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (context.size() != this.provisosTypes.size()) 
			throw new CTX_EXC(this.getSource(), "Missmatching number of provided provisos, expected " + this.provisosTypes.size() + ", got " + context.size());
		
		ProvisoUtil.mapNToN(this.provisosTypes, context);

		/* Copy proviso types with applied context */
		List<TYPE> clone = new ArrayList();
		for (TYPE t : this.provisosTypes) clone.add(t.clone());
		
		//System.out.println("Context");
		//clone.stream().forEach(x -> System.out.println(x.typeString()));
		
		ProvisoUtil.mapNTo1(this.returnType, clone);
		
		/* Copy return type with applied context */
		TYPE ret = this.returnType.clone().provisoFree();
		
		
		/* Apply to parameters */
		for (Declaration d : this.parameters) 
			d.setContext(clone);
		
		/* Apply to body */
		for (Statement s : this.body) 
			s.setContext(clone);
		
		//this.print(0, true);
		//System.out.println();
		
		
		/* Get proviso free of header provisos and return type copy */
		for (int i = 0; i < clone.size(); i++)
			clone.set(i, clone.get(i).provisoFree());
		
		
		/* Add mapping if it didnt exist */
		if (!this.containsMapping(clone)) 
			this.addProvisoMapping(ret, context);
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	public void printContexts() {
		System.out.println("Mappings: ");
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			System.out.print(this.provisosCalls.get(i).second.first.typeString() + " : ");
			List<TYPE> map = this.provisosCalls.get(i).second.second;
			for (TYPE t : map) System.out.print(t.typeString() + ", ");
			System.out.println();
		}
	}
	
	public boolean isActiveContext(List<TYPE> context) {
		return this.mappingIsEqual(context, this.provisosTypes);
	}
	
	public boolean containsMapping(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			if (map0.size() != map.size()) throw new SNIPS_EXC("Recieved proviso mapping length is not equal to expected length, expected " + map0.size() + ", but got " + map.size());
			
			if (this.mappingIsEqual(map0, map)) return true;
		}
		
		return false;
	}
	
	public boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) 
			isEqual &= map0.get(a).typeString().equals(map1.get(a).typeString());
		
		return isEqual;
	}
	
	public String getPostfix(List<TYPE> map) {
		/* Search for postfix, if match is found return postfix */
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			if (this.mappingIsEqual(map0, map)) 
				return this.provisosCalls.get(i).first;
		}
		
		return null;
	}
	
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			if (this.mappingIsEqual(map0, map))
				return this.provisosCalls.get(i).second.first;
		}
		
		throw new SNIPS_EXC("No mapping!");
	}
	
	public void addProvisoMapping(TYPE type, List<TYPE> context) {
		/* Proviso mapping already exists, just return */
		if (this.containsMapping(context)) return;
		else {
			/* Add proviso mapping, create new proviso postfix for mapping */
			String postfix = (context.isEmpty())? "" : LabelGen.getProvisoPostfix();
			this.provisosCalls.add(new Pair<String, Pair<TYPE, List<TYPE>>>(postfix, new Pair<TYPE, List<TYPE>>(type, context)));
		}
	}
	
} 
