package Imm.AST.Typedef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.ASM.ASMInstruction;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.Operands.RegOp.REG;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNNode;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class InterfaceTypedef extends SyntaxElement {

			/* ---< FIELDS >--- */
	public MODIFIER modifier;
	
	public NamespacePath path;
	
	public List<TYPE> proviso;
	
	public List<Function> functions;
	
	/* Interfaces this interface is implementing */
	public List<INTERFACE> implemented = new ArrayList();
	
	public List<StructTypedef> implementers = new ArrayList();

	public HashMap<String, ASMDataLabel> IIDLabelMap = new HashMap();
	
	public class InterfaceProvisoMapping {
		
		public String provisoPostfix;
		
		public List<TYPE> providedHeadProvisos;
		
		public InterfaceProvisoMapping(String provisoPostfix, List<TYPE> providedHeadProvisos) {
			this.provisoPostfix = provisoPostfix;
			this.providedHeadProvisos = providedHeadProvisos;
		}
		
	}
	
	public List<InterfaceProvisoMapping> registeredMappings = new ArrayList();
	
	public INTERFACE self;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InterfaceTypedef(NamespacePath path, List<TYPE> proviso, List<INTERFACE> implemented, List<Function> functions, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		
		this.proviso = proviso;
		this.functions = functions;
		
		this.modifier = modifier;
		
		this.implemented = implemented;
		
		this.initialize();
		
		this.self = new INTERFACE(this, this.proviso);
	}
	
	
			/* ---< METHODS >--- */
	public void initialize() {
		int c = 0;
		
		for (INTERFACE i : this.implemented) {
			InterfaceTypedef def = i.getTypedef();
			
			/* 
			 * For every function in the extension, copy the function, 
			 * adjust the path and add to own functions 
			 */
			for (Function f : def.functions) {
				
				/* Construct a namespace path that has this struct as base */
				NamespacePath base = this.path.clone();
				base.path.add(f.path.getLast());

				/* Create a copy of the function, but keep reference on body */
				Function f0 = f.clone();
				f0.path = base;
				
				f0.translateProviso(def.proviso, i.proviso);
				
				boolean override = false;
				for (Function fs : this.functions) 
					if (Function.signatureMatch(fs, f0, false, true, false))
						override = true;
				
				if (!override) {
					this.functions.add(c++, f0);
				}
			}
		}
	}
	
	public InterfaceProvisoMapping registerMapping(List<TYPE> newMapping) {
		
		/* Make sure that proviso sizes are equal, if not an error should've been thrown before */
		assert this.proviso.size() == newMapping.size() : "Expected " + this.proviso.size() + " proviso types, but got " + newMapping.size();
		
		for (InterfaceProvisoMapping m : this.registeredMappings) 
			if (ProvisoUtil.mappingIsEqual(newMapping, m.providedHeadProvisos))
				return m;
		
		/* Copy own provisos */
		List<TYPE> clone = new ArrayList();
		for (TYPE t : newMapping) 
			clone.add(t.clone());
		
		/* Create the new mapping and store it */
		InterfaceProvisoMapping mapping = new InterfaceProvisoMapping(LabelUtil.getProvisoPostfix(newMapping), clone);
		this.registeredMappings.add(mapping);
		
		return mapping;
	}

	/**
	 * Creates a copy of the function with the name of the given namespace path, performs
	 * proviso translation and returns the function. Proviso translation means that if the interface
	 * head has the provisos <K, V>, and the passed provisos are <M, N>, and if the function return type
	 * is <PROVISO:K>, the return type of the resulting function will be <PROVISO:M>.
	 */
	public Function requestFunction(NamespacePath path, List<TYPE> providedProvisos) {
		for (Function f : this.functions) {
			if (path.getLast().equals(f.path.getLast())) {
				/* Clone the function signature */
				Function f0 = f.cloneSignature();
				
				TYPE ret = f0.getReturnTypeDirect();
				f0.setReturnType(this.quickTranslate(ret, providedProvisos));
				
				for (int a = 0; a < f.parameters.size(); a++) {
					TYPE t0 = f0.parameters.get(a).getRawType();
					f0.parameters.get(a).setType(this.quickTranslate(t0, providedProvisos));
				}
				
				for (int a = 0; a < f.provisoTypes.size(); a++) {
					TYPE t0 = f0.provisoTypes.get(a);
					f0.provisoTypes.set(a, this.quickTranslate(t0, providedProvisos));
				}
				
				return f0;
			}
		}
		
		return null;
	}
	
	/**
	 * For the given type, re-map all provisos of the interface to the provided provisos and
	 * apply the new provisos to the type. Return the resulting type.
	 */
	private TYPE quickTranslate(TYPE t, List<TYPE> providedProvisos) {
		for (int k = 0; k < providedProvisos.size(); k++) {
			PROVISO definedProviso = (PROVISO) this.proviso.get(k).clone();
			t = t.remapProvisoName(definedProviso.placeholderName, providedProvisos.get(k).clone());
		}
		
		return t;
	}
	
	public void print(int d, boolean rec) {
		CompilerDriver.outs.println(Util.pad(d) + "Interface Typedef:<" + this.path + ">");
		
		if (rec) for (Function f : this.functions)
			f.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		Source temp = CompilerDriver.lastSource;
		CompilerDriver.lastSource = this.getSource();
		
		TYPE t = ctx.checkInterfaceTypedef(this);
		
		CompilerDriver.lastSource = temp;
		return t;
	}
	
	public SyntaxElement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optInterfaceTypedef(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Function f : this.functions) 
			result.addAll(f.visit(visitor));
		
		return result;
	}

	public void setContext(List<TYPE> context) throws CTEX_EXC {
		return;
	}
	
	public void loadIIDInReg(AsNNode node, REG reg, List<TYPE> context) {
		node.instructions.addAll(this.loadIIDInReg(reg, context));
	}
	
	public List<ASMInstruction> loadIIDInReg(REG reg, List<TYPE> context) {
		List<ASMInstruction> ins = new ArrayList();
		
		String postfix = LabelUtil.getProvisoPostfix(context);
		
		assert this.IIDLabelMap.get(postfix) != null : 
			"Attempted to load IID for a not registered mapping!";
		
		LabelOp operand = new LabelOp(this.IIDLabelMap.get(postfix));
		ins.add(new ASMLdrLabel(new RegOp(reg), operand, null));
		
		return ins;
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = "";
		
		if (this.modifier != MODIFIER.SHARED)
			s += this.modifier.toString().toLowerCase() + " ";
		
		s += "interface " + this.path;
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", "<", ">"));
		
		if (!this.implemented.isEmpty()) {
			s += " : ";
			s += this.implemented.stream().map(x -> x.codeString()).collect(Collectors.joining(", "));
		}
		
		s += " {";
		
		code.add(Util.pad(d) + s);
		
		code.add("");
		
		for (Function f : this.functions) {
			code.addAll(f.codePrint(d + this.printDepthStep));
			code.add("");
		}
		
		code.add(Util.pad(d) + "}");
		
		return code;
	}

	public SyntaxElement clone() {
		return this;
	}
} 
