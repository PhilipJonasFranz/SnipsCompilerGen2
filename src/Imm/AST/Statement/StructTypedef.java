package Imm.AST.Statement;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructTypedef extends SyntaxElement {

			/* --- FIELDS --- */
	public MODIFIER modifier;
	
	public NamespacePath path;
	
	public List<TYPE> proviso;
	
	private List<Declaration> fields;
	
	public StructTypedef extension = null;
	
	private STRUCT self;
	
	public int SID;
	
	public class StructProvisoMapping {
		
		public List<TYPE> providedHeadProvisos;
		
		public List<TYPE> effectiveFieldTypes;
		
		public StructProvisoMapping(List<TYPE> providedHeadProvisos, List<TYPE> effectiveFieldTypes) {
			this.providedHeadProvisos = providedHeadProvisos;
			this.effectiveFieldTypes = effectiveFieldTypes;
		}
		
	}
	
	public List<StructProvisoMapping> registeredMappings = new ArrayList();
	
	
			/* --- CONSTRUCTORS --- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructTypedef(NamespacePath path, int SID, List<TYPE> proviso, List<Declaration> declarations, StructTypedef extension, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		this.SID = SID;
		
		this.proviso = proviso;
		this.fields = declarations;
		this.extension = extension;
		
		this.modifier = modifier;
		
		this.self = new STRUCT(this, this.proviso);
	}
	
	
			/* --- METHODS --- */
	/**
	 * Request the declaration whiches name matches given path. Select the mapping
	 * that corresponds to given proviso types. If no such mapping exists, a new one
	 * is created.
	 * @return the declaration or null if the path did not match.
	 */
	public Declaration requestField(NamespacePath path, List<TYPE> providedProvisos) {
		StructProvisoMapping match = this.findMatch(providedProvisos);
		
		Declaration dec = null;
		for (int i = 0; i < this.fields.size(); i++) {
			if (this.fields.get(i).path.build().equals(path.build())) {
				/* Copy field and apply field type */
				dec = this.fields.get(i).clone();
				dec.setType(match.effectiveFieldTypes.get(i).provisoFree());
			}
		}
		
		return dec;
	}
	
	public List<Declaration> getFields() {
		return this.fields;
	}
	
	private StructProvisoMapping findMatch(List<TYPE> providedProvisos) {
		
		/* Make sure that proviso sizes are equal, if not an error should've been thrown before */
		assert(this.proviso.size() == providedProvisos.size());
		
		for (StructProvisoMapping m : this.registeredMappings) {
			boolean equal = true;
			for (int i = 0; i < m.providedHeadProvisos.size(); i++) 
				/* 1 to 1 match */
				equal &= m.providedHeadProvisos.get(i).isEqual(providedProvisos.get(i));
			
			if (equal) return m;
		}
		
		/* No mapping found, create new and return it */
		
		/* Copy own provisos */
		List<TYPE> clone = new ArrayList();
		for (TYPE t : this.proviso) 
			clone.add(t.clone());
		
		/* Map provided provisos to header */
		ProvisoUtil.mapNToN(clone, providedProvisos);
		
		/* Clone Struct field types, decs stay same anyway */
		List<TYPE> newActive = new ArrayList();
		for (Declaration d : this.fields) newActive.add(d.getType().clone());
		
		/* Mapped cloned header proviso with mapped types to field types */
		for (int i = 0; i < newActive.size(); i++) 
			ProvisoUtil.mapNTo1(newActive.get(i), clone);
		
		/* Remove provisos from field types */
		for (int i = 0; i < newActive.size(); i++) 
			newActive.set(i, newActive.get(i).provisoFree());

		//System.out.println(this.self.typeString() + " -> New Context registered");
		
		/* Create the new mapping and store it */
		StructProvisoMapping newMapping = new StructProvisoMapping(clone, newActive);
		this.registeredMappings.add(newMapping);
		
		return newMapping;
	}
	
	public void print(int d, boolean rec) {
		System.out.println(this.pad(d) + "Struct Typedef:SID=" + this.SID + "<" + this.path.build() + ">");
		if (rec) {
			for (Declaration dec : this.fields) 
				dec.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkStructTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		
	}

}
