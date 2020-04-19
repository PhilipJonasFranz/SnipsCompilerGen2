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
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public String structName;
	
	public STRUCT struct;
	
	public List<TYPE> proviso;
	
	public List<Declaration> fields;
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructTypedef(String id, List<TYPE> proviso, List<Declaration> declarations, Source source) {
		super(source);
		this.structName = id;
		this.proviso = proviso;
		this.fields = declarations;
		
		/* Call with reference on itself, struct type will be built when context checking */
		this.struct = new STRUCT(this, proviso);
	}
	
	
			/* --- METHODS --- */
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Struct Typedef <" + this.structName + ">");
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
				
		/* Apply new internal proviso mapping to capsuled declarations */
		for (Declaration dec : this.fields) {
			ProvisoManager.setContext(this.proviso, dec.getType());
		}
	}

	public void releaseContext() {
		for (Declaration dec : this.fields) dec.releaseContext();
	}
	
	public StructTypedef clone() {
		List<TYPE> prov0 = new ArrayList();
		for (TYPE t : this.proviso) prov0.add(t.clone());
		
		List<Declaration> dec0 = new ArrayList();
		for (Declaration dec : this.fields) dec0.add(dec.clone());
		
		StructTypedef clone = new StructTypedef(this.structName, prov0, dec0, this.getSource());
		
		return clone;
	}
	
	/** 
	 * Construct a new Struct Type based on this template and set given proviso as context.
	 */
	public STRUCT constructStructType(List<TYPE> proviso) throws CTX_EXCEPTION {
		STRUCT clone = this.struct.clone();
		
		/* Create copy of template */
		clone.typedef = this.clone();
		
		if (this.proviso.size() != proviso.size()) {
			throw new CTX_EXCEPTION(this.getSource(), "Missmatching number of provisos, expected " + this.proviso.size() + ", but got " + proviso.size());
		}
		
		clone.typedef.setContext(proviso);
		
		/* Assign proviso */
		clone.proviso = proviso;
		
		return clone;
	}
	
}
