package Imm.AST.Typedef;

import java.util.ArrayList;
import java.util.List;

import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTX_EXC;
import Imm.AST.Function;
import Imm.AST.SyntaxElement;
import Imm.AST.Statement.Declaration;
import Imm.AsN.AsNNode.MODIFIER;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.NamespacePath;
import Util.Source;

/**
 * This class represents a superclass for all AST-Nodes.
 */
public class StructTypedef extends SyntaxElement {

			/* ---< FIELDS >--- */
	public MODIFIER modifier;
	
	public NamespacePath path;
	
	public List<TYPE> proviso;
	
	private List<Declaration> fields;
	
	public List<Function> functions;
	
	/** List that contains functions from the functions list, that have been inherited */
	public List<Function> inheritedFunctions = new ArrayList();
	
	public StructTypedef extension = null;
	
	public List<INTERFACE> implemented;
	
	/** Proviso types provided by the typedef to the extension */
	public List<TYPE> extProviso;
	
	/* Contains all struct typedefs that extended from this struct */
	public List<StructTypedef> extenders = new ArrayList();
	
	public STRUCT self;
	
	/* 
	 * SID is assigned during context checking, in order to allow for efficient instanceof expressions.
	 */
	public int SID;
	
	public StructTypedef SIDNeighbour;
	
	public class StructProvisoMapping {
		
		public List<TYPE> providedHeadProvisos;
		
		public List<TYPE> effectiveFieldTypes;
		
		public StructProvisoMapping(List<TYPE> providedHeadProvisos, List<TYPE> effectiveFieldTypes) {
			this.providedHeadProvisos = providedHeadProvisos;
			this.effectiveFieldTypes = effectiveFieldTypes;
		}
		
	}
	
	public List<StructProvisoMapping> registeredMappings = new ArrayList();
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor.
	 * @param source See {@link #source}
	 */
	public StructTypedef(NamespacePath path, List<TYPE> proviso, List<Declaration> declarations, List<Function> functions, StructTypedef extension, List<INTERFACE> implemented, List<TYPE> extProviso, MODIFIER modifier, Source source) {
		super(source);
		this.path = path;
		
		this.proviso = proviso;
		this.fields = declarations;
		this.functions = functions;
		
		this.extension = extension;
		this.extProviso = extProviso;
		
		this.implemented = implemented;
		
		for (INTERFACE i : this.implemented) 
			i.getTypedef().implementers.add(this);
		
		this.modifier = modifier;
		this.self = new STRUCT(this, this.proviso);
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * This method is called once the entire struct typedef is parsed. After all function 
	 * in this struct are added, functions from the extensions are added, with respect to 
	 * overwritten functions.
	 */
	public void postInitialize() {
		int c = 0;
		
		/* Add this typedef to extenders of extension */
		if (this.extension != null) {
			this.extension.extenders.add(this);
			
			/* Register this struct typedef at all implemented interfaces from the extension */
			for (INTERFACE i : this.extension.implemented) {
				boolean contained = false;
				for (TYPE t : this.implemented)
					if (t.isEqual(i))
						contained = true;
				
				/* 
				 * The Struct typedef may define implementation by itself, 
				 * and may even override the provisos. In this case leave the
				 * existing interface reference.
				 */
				if (!contained) {
					/* If not contained, add to implemented and register at interface */
					INTERFACE iclone = i.clone();
					
					/* Translate: Extension Interface Proviso -> Extension Head Proviso */
					List<TYPE> pClone = new ArrayList();
					for (int a = 0; a < this.extension.proviso.size(); a++) {
						TYPE translated = ProvisoUtil.translate(i.getTypedef().proviso.get(a).clone(), i.getTypedef().proviso, this.extension.proviso);
						pClone.add(translated);
					}
					
					/* Translate: Extension Head Proviso -> Extension Proviso */
					for (int a = 0; a < iclone.proviso.size(); a++) {
						TYPE translated = ProvisoUtil.translate(iclone.proviso.get(a), pClone, this.proviso);
						iclone.proviso.set(a, translated);
					}
					
					this.implemented.add(iclone);
					i.getTypedef().implementers.add(this);
				}
			}
			
			/* 
			 * For every function in the extension, copy the function, 
			 * adjust the path and add to own functions 
			 */
			for (Function f : this.extension.functions) {
				/* Ignore static functions */
				if (f.modifier == MODIFIER.STATIC) continue;
				
				/* Construct a namespace path that has this struct as base */
				NamespacePath base = this.path.clone();
				base.path.add(f.path.getLast());

				/* Create a copy of the function, but keep reference on body */
				Function f0 = f.clone();
				f0.path = base;
				
				f0.translateProviso(this.extension.proviso, this.extProviso);
				
				/* Temporarily disable the provisoFree() part in the isEqualExtended() method */
				STRUCT.useProvisoFreeInCheck = false;
				
				boolean override = false;
				for (Function fs : this.functions) 
					if (Function.signatureMatch(fs, f0, false, true))
						override = true;
				
				if (!override) {
					this.functions.add(c++, f0);
					this.inheritedFunctions.add(f0);
				}

				STRUCT.useProvisoFreeInCheck = true;
			}
		}
	}
	
	/**
	 * Assign SIDs and neighbours to the StructTypedefs based on the location
	 * in the extension tree. SIDs are unique, as well as the neighbours.
	 */
	public int propagateSIDs(int start, StructTypedef neighbour) {
		this.SID = start;
		start++;
		this.SIDNeighbour = neighbour;
		
		if (!this.extenders.isEmpty()) {
			if (this.extenders.size() == 1) 
				start = this.extenders.get(0).propagateSIDs(start, neighbour);
			else {
				/* Apply to first n - 1 */
				for (int i = 1; i < this.extenders.size(); i++) { 
					start = this.extenders.get(i - 1).propagateSIDs(start, this.extenders.get(i));
					
					/* Set neighbour of n - 1 to n */
					this.extenders.get(i - 1).SIDNeighbour = this.extenders.get(i);
				}
				
				/* Apply to last */
				this.extenders.get(this.extenders.size() - 1).propagateSIDs(start, neighbour);
			}
		}
		
		return start;
	}
	
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
		assert this.proviso.size() == providedProvisos.size() : "Expected " + this.proviso.size() + " proviso types, but got " + providedProvisos.size();
		
		for (StructProvisoMapping m : this.registeredMappings) {
			boolean equal = true;
			
			for (int i = 0; i < m.providedHeadProvisos.size(); i++) 
				/* 
				 * 1 to 1 match, match type string since we look for perfect 
				 * match, void proviso types could disrupt that.
				 */
				equal &= m.providedHeadProvisos.get(i).typeString().equals(providedProvisos.get(i).typeString());
			
			if (equal) return m;
		}
		
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

		/* Create the new mapping and store it */
		StructProvisoMapping newMapping = new StructProvisoMapping(clone, newActive);
		this.registeredMappings.add(newMapping);
		
		return newMapping;
	}
	
	public void print(int d, boolean rec) {
		String s = this.pad(d) + "Struct Typedef:SID=" + this.SID + "<" + this.path.build() + ">";
		
		if (this.extension != null)
			s += ":extends:" + this.extension.path.build() + ",";
		
		if (!this.implemented.isEmpty()) {
			if (this.extension != null)
				s += ",";
			else 
				s += ":";
			
			s += "implements:";
		}
		
		for (INTERFACE def : this.implemented)
			s += def.getTypedef().path.build() + ",";
		
		if (this.extension != null || !this.implemented.isEmpty())
			s = s.substring(0, s.length() - 1);
		
		System.out.println(s);
		
		if (rec) {
			for (Declaration dec : this.fields) 
				dec.print(d + this.printDepthStep, rec);
		
			for (Function f : this.functions)
				f.print(d + this.printDepthStep, rec);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTX_EXC {
		return ctx.checkStructTypedef(this);
	}

	public void setContext(List<TYPE> context) throws CTX_EXC {
		return;
	}

} 
