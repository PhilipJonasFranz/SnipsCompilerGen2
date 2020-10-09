package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
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
	}
	
	
			/* --- METHODS --- */
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
