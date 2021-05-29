package Imm.AST;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Exc.SNIPS_EXC;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.AST.Statement.CompoundStatement;
import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.ReturnStatement;
import Imm.AST.Statement.Statement;
import Imm.AST.Typedef.InterfaceTypedef;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Res.Const;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.MODIFIER;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class Function extends CompoundStatement {

			/* ---< NESTED >--- */
	/**
	 * Used to pass compare-criteria when comparing two function
	 * signatures.
	 */
	public static enum SIG_M_CRIT {

		/** Match parameter names */
		PARAM_NAMES,

		/** Call proviso free when comparing parameter types */
		PROVISO_FREE_IN_PARAMS,

		/** Match the full namespace paths of the functions */
		FULL_NAMES,

		/** Check if the amount of proviso heads is equal */
		PROVISO_AMOUNT;

	}

	/**
	 * A proviso mapping is used to store a unique mapping that was
	 * applied to this function or ressource. The mapping stores a unique
	 * postfix, return type and the provided proviso types. All types are
	 * stored as copies and proviso free.
	 */
	public static class ProvisoMapping {

				/* ---< FIELDS >--- */
		/**
		 * The return type of the function when this mapping is applied to it.
		 */
		public TYPE returnType;

		/**
		 * The provided proviso types.
		 */
		public List<TYPE> provisoMapping;


				/* ---< CONSTRUCTORS >--- */
		public ProvisoMapping(TYPE returnType, List<TYPE> provisoMapping) {
			this.returnType = returnType;
			this.provisoMapping = provisoMapping;

			assert provisoMapping.stream().noneMatch(x -> x.hasProviso()) : "Found proviso type in proviso mapping!";
		}

		public String getProvisoPostfix() {
			return LabelUtil.getProvisoPostfix(this.provisoMapping);
		}

	}

			/* ---< FIELDS >--- */
	/**
	 * Set to the interface typedef when this function is
	 * a function head defined in this interface.
	 */
	public InterfaceTypedef definedInInterface;

	/**
	 * Set to the struct typedef when this function is
	 * nested inside a struct.
	 */
	public StructTypedef definedInStruct;

	/**
	 * Set to true if this function is implemented from an interface.
	 * When the Interface Relay Table branches to the function, the value
	 * in R10 needs to be set to R10 to prevent wrong behaviour.
	 */
	public boolean requireR10Reset = false;

	/**
	 * The flattened namespace path of this function.
	 */
	public NamespacePath path;

	/**
	 * List of the provisos types this function is templated with
	 */
	public List<TYPE> provisoTypes;

	/**
	 * A list that contains the combinations of types this function was templated with.
	 * Stores Label Gen Proviso Postfix, Return Type and proviso types.
	 */
	public List<ProvisoMapping> provisosCalls = new ArrayList();

	/**
	 * The visibility modifier specified in the function declaration.
	 */
	public MODIFIER modifier;

	/**
	 * The return type of this function. May contain provisos.
	 */
	private TYPE returnType;

	/**
	 * The parameters of this function.
	 */
	public List<Declaration> parameters;

	/**
	 * The types this function signals.
	 */
	public List<TYPE> signalsTypes;

	/**
	 * The types this function actually signals.
	 */
	public List<TYPE> actualSignals = new ArrayList();

	/* Flags for lambda targeting */
	public Declaration lambdaDeclaration;

	/**
	 * Set to true when this function is used as a function
	 * head, or is capsuled within a FUNC type.
	 */
	public boolean isLambdaHead = false;

	/**
	 * Set to true when at least one return statement
	 * within the body of the function has a return value.
	 */
	public boolean hasReturn = false;

	/**
	 * Set during context checking. Used to make sure that at least
	 * one return statement.
	 */
	public ReturnStatement noReturn = null;

	/**
	 * Unique ID of this function.
	 */
	public int UID = LabelUtil.getUID();

	/**
	 * Set during AST_OPT phase, contains reference to the function in
	 * the duplicate AST that is used to probe optimizations, before they
	 * are passed to this function.
	 */
	public Function ASTOptCounterpart;

	public int LAST_UPDATE = 0;

	/**
	 * A list that is set during context checking that lists all statements
	 * in the function body that are not providing a return statement. This
	 * is computed using the noReturnStatement() function from Matcher.
	 * The algorithm chooses the highest-scoped statement that is missing
	 * a return statement, for example if an if-statement in an if-statement
	 * is missing a return statement, but the outer if provides one, the inner
	 * one is discarded. Otherwise, if the outer does not provide a return,
	 * the outer one is chosen since it is in a higher scope.
	 */
	public List<Statement> statementsWithoutReturn = new ArrayList();

	/**
	 * If this function is not null, and the body of this function is null,
	 * the cast of this function will simply relay to this function.
	 */
	public Function inheritLink = null;

	/**
	 * If set to true, the '...@UID' will be included in the function
	 * head asm label.
	 */
	public boolean requireUIDInLabel = false;

	public HashMap<String, ASMLabel> headLabelMap = new HashMap();


			/* ---< CONSTRUCTORS >--- */
	public Function(TYPE returnType, NamespacePath path, List<TYPE> proviso, List<Declaration> parameters, List<TYPE> signalsTypes, List<Statement> statements, MODIFIER modifier, Source source) {
		super(statements, source);
		this.returnType = returnType;
		this.path = path;
		this.parameters = parameters;

		this.signalsTypes = signalsTypes;

		this.modifier = modifier;

		this.provisoTypes = proviso;

		if (path.build().equals("main"))
			/* Add default mapping */
			this.addProvisoMapping(null, new ArrayList());

		this.UID = Math.abs(this.signatureToString().hashCode());
	}


			/* ---< METHODS >--- */
	public void print(int d, boolean rec) {
		CompilerDriver.outs.print(Util.pad(d) + "<" + this.returnType + "> " + this.path);

		if (!this.provisoTypes.isEmpty()) {
			CompilerDriver.outs.print("<");
			for (int i = 0; i < this.provisoTypes.size(); i++) {
				CompilerDriver.outs.print(this.provisoTypes.get(i).typeString());
				if (i < this.provisoTypes.size() - 1) CompilerDriver.outs.print(", ");
			}
			CompilerDriver.outs.print(">");
		}

		CompilerDriver.outs.print("(");
		for (int i = 0; i < this.parameters.size(); i++) {
			Declaration dec = parameters.get(i);
			CompilerDriver.outs.print("<" + dec.getRawType() + "> " + dec.path);
			if (i < this.parameters.size() - 1) CompilerDriver.outs.print(", ");
		}
		CompilerDriver.outs.print(")");

		if (!this.signalsTypes.isEmpty()) {
			CompilerDriver.outs.print(" signals ");
			for (int i = 0; i < this.signalsTypes.size(); i++) {
				CompilerDriver.outs.print(this.signalsTypes.get(i).typeString());
				if (i < this.signalsTypes.size() - 1) CompilerDriver.outs.print(", ");
			}
		}

		if (this.inheritLink != null)
			CompilerDriver.outs.print(" overrides <" + this.inheritLink.signatureToString() + ">");

		if (!this.activeAnnotations.isEmpty())
			CompilerDriver.outs.print(" " + this.activeAnnotations);

		CompilerDriver.outs.println();

		if (rec && body != null) for (Statement s : body)
			s.print(d + this.printDepthStep, true);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);

		TYPE t = ctx.checkFunction(this);

		ctx.popTrace();
		return t;
	}

	public Function opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optFunction(this);
	}

	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();

		if (visitor.visit(this))
			result.add((T) this);

		if (this.body != null) {
			for (Statement s : this.body) {
				result.addAll(s.visit(visitor));
			}
		}

		return result;
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

	/**
	 * Sets the return type of this function.
	 */
	public void setReturnType(TYPE t) {
		this.returnType = t;
	}

	/**
	 * Return wether this function signals exceptions or not.
	 */
	public boolean signals() {
		return !this.signalsTypes.isEmpty();
	}


			/* ---< PROVISO RELATED >--- */
	/**
	 * Set given context to the function, including parameters, return type and body.
	 * Check if the given mapping already existed in the mapping pool, if not create a
	 * new proviso-free mapping and store it in the proviso calls.
	 */
	public void setContext(List<TYPE> context) throws CTEX_EXC {
		if (context.size() != this.provisoTypes.size())
			throw new CTEX_EXC(this, Const.MISSMATCHING_NUMBER_OF_PROVISOS, this.provisoTypes.size(), context.size());

		ProvisoUtil.mapNToN(this.provisoTypes, context);

		/* Copy proviso types with applied context */
		List<TYPE> clone = new ArrayList();
		for (TYPE t : this.provisoTypes) clone.add(t.clone());

		ProvisoUtil.mapNTo1(this.returnType, clone);

		/* Copy return type with applied context */
		TYPE ret = this.returnType.clone().provisoFree();


		/* Apply to parameters */
		for (Declaration d : this.parameters)
			d.setContext(clone);

		/* Apply to body */
		if (this.body != null)
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
		for (ProvisoMapping provisosCall : this.provisosCalls) {
			List<TYPE> map0 = provisosCall.provisoMapping;

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
		if (map == null || map.isEmpty()) return "";

		/* Search for postfix, if match is found return postfix */
		for (ProvisoMapping provisosCall : this.provisosCalls) {
			List<TYPE> map0 = provisosCall.provisoMapping;

			if (ProvisoUtil.mappingIsEqualProvisoFree(map0, map)) {
				return LabelUtil.getProvisoPostfix(provisosCall.provisoMapping);
			}
		}

		return null;
	}
	
	/**
	 * Adds a new proviso mapping to this function if the given proviso mapping
	 * does not exist already.
	 * @param returnType The return type of the new mapping.
	 * @param context The proviso types of the new mapping.
	 */
	public void addProvisoMapping(TYPE returnType, List<TYPE> context) {
		if (!this.containsMapping(context)) {
			TYPE retTypeClone = null;
			if (returnType != null) retTypeClone = returnType.clone().provisoFree();
			
			List<TYPE> contextClone = new ArrayList();
			for (TYPE t : context)
				contextClone.add(t.clone().provisoFree());
			
			this.provisosCalls.add(new ProvisoMapping(retTypeClone, contextClone));
		}
	}

	/**
	 * Translates this function from the source proviso space to the target proviso space.<br>
	 * <br>
	 * When to use this function: A function nested in struct type A is re-located to type B. B extends A.<br>
	 * Then the function needs to be proviso translated, with f.translate(A.provisos, B.provisos).<br>
	 * <br>
	 * This will cause the functions provisos to be adjusted to the provisos in B.<br>
	 * <br>
	 * Note: Only does this to the functions signature, not the body.
	 * 
	 * @param source The proviso heads of the ressource where the function was located
	 * @param target The proviso heads of the ressource where the function is re-located to
	 */
	public void translateProviso(List<TYPE> source, List<TYPE> target) {
		this.returnType = ProvisoUtil.translate(this.returnType, source, target);
		
		for (int i = 0; i < this.provisoTypes.size(); i++)
			this.provisoTypes.set(i, ProvisoUtil.translate(this.provisoTypes.get(i), source, target));
		
		for (Declaration d : this.parameters)
			d.setType(ProvisoUtil.translate(d.getRawType(), source, target));
		
		// TODO: Also translate body
	}
	
	
			/* ---< METHOD >--- */
	/**
	 * Returns the return type that corresponds to the given mapping.
	 * @param map The proviso map which is matched to the registered proviso maps to get the return type from.
	 * @return The return type.
	 * @throws SNIPS_EXC If no registered mapping is equal to the given mapping.
	 */
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (ProvisoMapping provisosCall : this.provisosCalls) {
			List<TYPE> map0 = provisosCall.provisoMapping;

			if (ProvisoUtil.mappingIsEqualProvisoFree(map0, map))
				return provisosCall.returnType;
		}
		
		throw new SNIPS_EXC(Const.NO_MAPPING_EQUAL_TO_GIVEN_MAPPING);
	}
	
	/**
	 * Clones the function's signature. This includes deep copies of:
	 * 
	 * - Return Type
	 * - Namespace Path
	 * - Proviso Types
	 * - Parameters
	 * - Signal Types
	 * 
	 * As well as copies of all other fields and flags, except the body of the function,
	 * which is left empty.
	 * 
	 * @return The newly created function signature.
	 */
	public Function cloneSignature() {
		List<TYPE> provClone = new ArrayList();
		for (TYPE t : this.provisoTypes)
			provClone.add(t.clone());
		
		List<Declaration> params = new ArrayList();
		for (Declaration d : this.parameters)
			params.add(d.clone());
		
		List<TYPE> signalsTypes = new ArrayList();
		for (TYPE t : this.signalsTypes)
			signalsTypes.add(t.clone());
		
		/* Clone the function signature */
		Function f = new Function(this.getReturnTypeDirect().clone(), this.path.clone(), provClone, params, signalsTypes, new ArrayList(), this.modifier, this.getSource().clone());

		f.definedInStruct = this.definedInStruct;
		f.definedInInterface = this.definedInInterface;

		f.UID = this.UID;
		return f;
	}

	/**
	 * Compares the signatures of the two given functions. This includes name, parameters
	 * and return type. Returns true if the signatures match.
	 * @param f0 The first function.
	 * @param f1 The second function to match against the first.
	 * @param criteria A list of criteria that should be taken into account.
	 * @return True iff the signatures match with the specified flags.
	 */
	public static boolean signatureMatch(Function f0, Function f1, SIG_M_CRIT...criteria) {
		boolean match = true;

		/* Match function name, not namespace path */
		match = f0.path.getLast().equals(f1.path.getLast());

		if (Arrays.stream(criteria).anyMatch(x -> x == SIG_M_CRIT.FULL_NAMES)) match &= f0.path.equals(f1.path);

		/* Match function modifier */
		match &= f0.modifier == f1.modifier;

		/* Compare parameters */
		if (f0.parameters.size() == f1.parameters.size()) {
			for (int i = 0; i < f0.parameters.size(); i++) {
				Declaration d0 = f0.parameters.get(i);
				Declaration d1 = f1.parameters.get(i);

				/* Also match names if flag is set */
				if (Arrays.stream(criteria).anyMatch(x -> x == SIG_M_CRIT.PARAM_NAMES)) match &= d0.path.getLast().equals(d1.path.getLast());

				if (Arrays.stream(criteria).anyMatch(x -> x == SIG_M_CRIT.PROVISO_FREE_IN_PARAMS)) match &= d0.getType().isEqual(d1.getType());
				else match &= d0.getRawType().isEqual(d1.getRawType());
			}
		}
		else match = false;

		/* Compare the return type */
		match &= f0.getReturnTypeDirect().isEqual(f1.getReturnTypeDirect());

		/*
		 * Check if the same amount of possible proviso are present. We do not care about the
		 * context or proviso names. When a struct is extending another struct, the proviso
		 * names are overwritten. The function is thus inherited, but matches the parent's
		 * signature.
		 */
		if (Arrays.stream(criteria).anyMatch(x -> x == SIG_M_CRIT.PROVISO_AMOUNT)) match &= f0.provisoTypes.size() == f1.provisoTypes.size();

		return match;
	}
	
	/**
	 * Builds the string that this function can be called with. The label will contain:<br>
	 * - The name of the function<br>
	 * - '@UID'<br>
	 * - The proviso postfix<br>
	 * @param provisos The proviso mapping to determine the proviso postfix.
	 * @return The generated string.
	 */
	public String buildCallLabel(List<TYPE> provisos) {
		/* Excluded from UIDs in the label are the main function and any dynamic library functions like operators and memory routines */
		return this.path.build() + ((this.path.build().startsWith("__") || this.path.build().equals("main")|| 
									 this.path.build().equals("resv")|| this.path.build().equals("free") || this.path.build().equals("isa") || this.path.build().equals("isar") || 
									 this.path.build().equals("init")|| this.path.build().equals("hsize") || !this.requireUIDInLabel)? "" : "_" + this.UID)
				+ this.getProvisoPostfix(provisos);
	}
	
	public String buildInheritedCallLabel(List<TYPE> provisos) {
		NamespacePath path = this.inheritLink.path;
		
		/* Excluded from UIDs in the label are the main function and any dynamic library functions like operators and memory routines */
		return path.build() + ((path.build().startsWith("__") || path.build().equals("main")|| 
									 path.build().equals("resv")|| path.build().equals("free") || path.build().equals("isa") || path.build().equals("isar") || 
									 path.build().equals("init")|| path.build().equals("hsize") || !this.requireUIDInLabel)? "" : "_" + this.UID)
				+ this.getProvisoPostfix(provisos);
	}

	public Function clone() {
		Function f = this.cloneSignature();
		
		if (this.body != null) {
			List<Statement> clone = new ArrayList();
			for (Statement s : this.body) clone.add(s.clone());
			f.body = clone;
		}
		
		f.copyDirectivesFrom(this);
		return f;
	}
	
	public String signatureToString() {
		String s = "";
		
		if (this.modifier != MODIFIER.SHARED)
			s += this.modifier.toString().toLowerCase() + " ";
		
		s += this.returnType.codeString() + " ";
		
		s += this.path.build();
		
		if (!this.provisoTypes.isEmpty()) 
			s += this.provisoTypes.stream().map(TYPE::codeString).collect(Collectors.joining(", ", "<", ">"));
		
		s += "(";
		
		if (!this.parameters.isEmpty()) 
			s += this.parameters.stream().map(x -> x.getType().codeString() + " " + x.path)
				.collect(Collectors.joining(", "));
		
		s += ")";
		
		return s;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = this.signatureToString();
		
		if (this.body != null) {
			s += " {";
			code.add(Util.pad(d) + s);
			for (Statement s0 : this.body) {
				code.addAll(s0.codePrint(d + this.printDepthStep));
			}
			code.add(Util.pad(d) + "}");
		}
		else code.add(Util.pad(d) + s + ";");
		
		return code;
	}
	
	public boolean isConstructor() { 
		return this.modifier == MODIFIER.STATIC && 
				this.path.build().endsWith("create") && 
				this.getReturnType().isStruct();
	}

	public boolean isDestructor() {
		return this.modifier == MODIFIER.SHARED &&
				this.path.build().endsWith("destroy") &&
				this.getReturnType().isVoid();
	}

} 
