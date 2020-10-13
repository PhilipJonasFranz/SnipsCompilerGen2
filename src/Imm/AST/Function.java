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
import Imm.AST.Statement.InterfaceTypedef;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
import Res.Const;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* --- NESTED --- */
	public class ProvisoMapping {
		
		public String provisoPostfix;
		
		public TYPE returnType;
		
		public List<TYPE> provisoMapping;
		
		public ProvisoMapping(String provisoPostfix, TYPE returnType, List<TYPE> provisoMapping) {
			this.provisoPostfix = provisoPostfix;
			this.returnType = returnType;
			this.provisoMapping = provisoMapping;
		}
		
	}
	
			/* --- FIELDS --- */
	/** 
	 * Set to the interface typedef when this function is 
	 * a function head defined in this interface. 
	 */
	public InterfaceTypedef definedInInterface;
	
	/**
	 * Set to true if this function is implemented from an interface.
	 * When the Interface Relay Table branches to the function, the value
	 * in R10 needs to be set to R10 to prevent wrong behaviour.
	 */
	public boolean requireR10Reset = false;
	
	public boolean wasCalled = false;
	
	/**
	 * The flattened namespace path of this function.
	 */
	public NamespacePath path;
	
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** 
	 * A list that contains the combinations of types this function was templated with. 
	 * Stores Label Gen Proviso Postfix, Return Type and proviso types.
	 */
	public List<ProvisoMapping> provisosCalls = new ArrayList();
	
	/**
	 * The visibility modifier specified in the function declaration.
	 */
	public MODIFIER modifier = MODIFIER.SHARED;
	
	private TYPE returnType;
	
	public List<Declaration> parameters;
	
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
	 * Returns the current return type, proviso-free.
	 */
	public TYPE getReturnType() {
		return this.returnType.provisoFree();
	}
	
	/** 
	 * Returns the current return type, without freeing it of provisos.
	 */
	public TYPE getReturnTypeDirect() {
		return this.returnType;
	}
	
	public void setReturnType(TYPE t) {
		this.returnType = t;
	}
	
	/**
	 * Return wether this function signals exceptions or not.
	 */
	public boolean signals() {
		return !this.signalsTypes.isEmpty();
	}

	
			/* --- PROVISO RELATED --- */
	/**
	 * Set given context to the function, including parameters, return type and body.
	 * Check if the given mapping already existed in the mapping pool, if not create a 
	 * new proviso-free mapping and store it in the proviso calls.
	 */
	public void setContext(List<TYPE> context) throws CTX_EXC {
		if (context.size() != this.provisosTypes.size()) 
			throw new CTX_EXC(this.getSource(), Const.MISSMATCHING_NUMBER_OF_PROVISOS, this.provisosTypes.size(), context.size());
		
		ProvisoUtil.mapNToN(this.provisosTypes, context);

		/* Copy proviso types with applied context */
		List<TYPE> clone = new ArrayList();
		for (TYPE t : this.provisosTypes) clone.add(t.clone());
		
		ProvisoUtil.mapNTo1(this.returnType, clone);
		
		/* Copy return type with applied context */
		TYPE ret = this.returnType.clone().provisoFree();
		
		
		/* Apply to parameters */
		for (Declaration d : this.parameters) 
			d.setContext(clone);
		
		/* Apply to body */
		for (Statement s : this.body) 
			s.setContext(clone);
		
		/* Get proviso free of header provisos and return type copy */
		for (int i = 0; i < clone.size(); i++)
			clone.set(i, clone.get(i).provisoFree());
		
		
		/* Add mapping if it didnt exist */
		if (!this.containsMapping(clone)) 
			this.addProvisoMapping(ret, context);
	}
	
	/**
	 * Checks if given proviso mapping is already registered.
	 * @param map The proviso mapping to be checked against.
	 * @return True if an equal mapping is already registered, false if not.
	 */
	public boolean containsMapping(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).provisoMapping;
			
			if (map0.size() != map.size()) 
				throw new SNIPS_EXC(Const.RECIEVED_MAPPING_LENGTH_NOT_EQUAL, map0.size(), map.size());
			
			if (ProvisoUtil.mappingIsEqualProvisoFree(map0, map)) return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the proviso postfix that corresponds to the given mapping.
	 * This postfix has the form of _Px, where x is a number.
	 * @param map The proviso map which is matched to the registered proviso maps to get the postfix from.
	 * @return The postfix as a string or null if the mapping is not found.
	 */
	public String getProvisoPostfix(List<TYPE> map) {
		/* Search for postfix, if match is found return postfix */
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).provisoMapping;
			
			if (ProvisoUtil.mappingIsEqualProvisoFree(map0, map)) 
				return this.provisosCalls.get(i).provisoPostfix;
		}
		
		return null;
	}
	
	/**
	 * Returns the return type that corresponds to the given mapping.
	 * @param map The proviso map which is matched to the registered proviso maps to get the return type from.
	 * @return The return type.
	 * @throws SNIPS_EXC If no registered mapping is equal to the given mapping.
	 */
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).provisoMapping;
			
			if (ProvisoUtil.mappingIsEqualProvisoFree(map0, map))
				return this.provisosCalls.get(i).returnType;
		}
		
		throw new SNIPS_EXC(Const.NO_MAPPING_EQUAL_TO_GIVEN_MAPPING);
	}
	
	public void addProvisoMapping(TYPE type, List<TYPE> context) {
		/* Proviso mapping already exists, just return */
		if (this.containsMapping(context)) return;
		else {
			/* Add proviso mapping, create new proviso postfix for mapping */
			String postfix = (context.isEmpty())? "" : LabelGen.getProvisoPostfix();
			this.provisosCalls.add(new ProvisoMapping(postfix, type, context));
		}
	}
	
	public Function cloneSignature() {
		List<TYPE> provClone = new ArrayList();
		for (TYPE t : this.provisosTypes)
			provClone.add(t.clone());
		
		List<Declaration> params = new ArrayList();
		for (Declaration d : this.parameters)
			params.add(d.clone());
		
		List<TYPE> signalsTypes = new ArrayList();
		for (TYPE t : this.signalsTypes)
			signalsTypes.add(t.clone());
		
		/* Clone the function signature */
		return new Function(this.getReturnTypeDirect().clone(), this.path.clone(), provClone, params, this.signals(), signalsTypes, new ArrayList(), this.modifier, this.getSource().clone());
	}
	
} 
