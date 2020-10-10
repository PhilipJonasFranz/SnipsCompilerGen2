package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
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
	
	/* Contains all struct typedefs that extended from this struct */
	public List<InterfaceTypedef> extenders = new ArrayList();

	public class InterfaceProvisoMapping {
		
		public List<TYPE> providedHeadProvisos;
		
		public InterfaceProvisoMapping(List<TYPE> providedHeadProvisos) {
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
		if (rec) {
			for (Function f : this.functions)
				f.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkInterfaceTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

} 
