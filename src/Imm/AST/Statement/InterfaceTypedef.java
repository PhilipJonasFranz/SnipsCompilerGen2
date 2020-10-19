package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class InterfaceTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public MODIFIER modifier;
	
	public NamespacePath path;
	
	public List<TYPE> proviso;
	
	public List<Function> functions;
	
	public List<StructTypedef> implementers = new ArrayList();

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
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public InterfaceTypedef(NamespacePath path, List<TYPE> proviso, List<Function> functions, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		
		this.proviso = proviso;
		this.functions = functions;
		
		this.modifier = modifier;
		
		this.self = new INTERFACE(this, this.proviso);
	}
	
	
			/* --- METHODS --- */
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
		InterfaceProvisoMapping mapping = new InterfaceProvisoMapping(LabelUtil.getProvisoPostfix(), clone);
		this.registeredMappings.add(mapping);
		
		//System.out.print("\nRegistered Interface Mapping: ");
		
		String s = "";
		for (TYPE t : newMapping)
			s += t.typeString() + ",";
		if (!newMapping.isEmpty())
			s = s.substring(0, s.length() - 1);
		
		//System.out.println(s);
		
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
				
				for (int a = 0; a < f.provisosTypes.size(); a++) {
					TYPE t0 = f0.provisosTypes.get(a);
					f0.provisosTypes.set(a, this.quickTranslate(t0, providedProvisos));
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
		System.out.println(this.pad(d) + "Interface Typedef:<" + this.path.build() + ">");
		
		if (rec) for (Function f : this.functions)
			f.print(d + this.printDepthStep, rec);
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkInterfaceTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

} 
