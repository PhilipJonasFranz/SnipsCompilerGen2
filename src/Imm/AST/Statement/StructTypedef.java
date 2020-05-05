package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoManager;
import Exc.CTX_EXCEPTION;
import Imm.AST.SyntaxElement;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public NamespacePath path;
	
	public STRUCT struct;
	
	public List<TYPE> proviso;
	
	public List<Declaration> fields;
	
	public StructTypedef extension = null;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructTypedef(NamespacePath path, List<TYPE> proviso, List<Declaration> declarations, StructTypedef extension, Source source) {
		super(source);
		this.path = path;
		this.proviso = proviso;
		this.fields = declarations;
		this.extension = extension;
		
		/* Call with reference on itself, struct type will be built when context checking */
		this.struct = new STRUCT(this, proviso);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Struct Typedef <" + this.path.build() + ">");
		if (rec) {
			for (Declaration dec : this.fields) {
				dec.print(d + this.printDepthStep, rec);
			}
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXCEPTION {
		return ctx.checkStructTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		/* Apply context to internal proviso */
		for (int i = 0; i < this.proviso.size(); i++) {
			PROVISO p0 = (PROVISO) this.proviso.get(i);
			
			List<TYPE> con0 = new ArrayList();
			con0.add(context.get(i));
			
			ProvisoManager.setContext(con0, p0);
		}
		
		for (int i = 0; i < context.size(); i++) {
			context.set(i, ProvisoManager.setHiddenContext(context.get(i)));
		}
			
		/* Apply new internal proviso mapping to capsuled declarations */
		for (Declaration dec : this.fields) {
			ProvisoManager.setContext(context, dec.getType());
		}
	}

	public void releaseContext() {
		for (Declaration dec : this.fields) dec.releaseContext();
	}
	
	public StructTypedef clone() {
		List<TYPE> prov0 = new ArrayList();
		for (TYPE t : this.proviso) prov0.add(t.clone());
		
		List<Declaration> dec0 = new ArrayList();
		for (Declaration dec : this.fields) {
			dec0.add(dec.clone());
		}
		
		StructTypedef clone = new StructTypedef(this.path, prov0, dec0, this.extension, this.getSource());
		
		assert(clone.fields.size() == this.fields.size());
		
		return clone;
	}
	
	/** 
	 * Construct a new Struct Type based on this template and set given proviso as context.
	 */
	public STRUCT constructStructType(List<TYPE> proviso) throws CTX_EXCEPTION {
		/* Do not clone type, struct types and typedef are already cloned during parsing */
		
		if (this.proviso.size() != proviso.size()) {
			throw new CTX_EXCEPTION(this.getSource(), "Missmatching number of provisos, expected " + this.proviso.size() + ", but got " + proviso.size());
		}
		
		this.setContext(proviso);
		
		/* Assign proviso */
		this.struct.proviso = proviso;
		
		return this.struct;
	}
	
}
