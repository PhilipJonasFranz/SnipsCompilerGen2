package Imm.AST.Typedef;

import CGen.Util.LabelUtil;
import Ctx.ContextChecker;
import Ctx.Util.ProvisoUtil;
import Exc.CTEX_EXC;
import Exc.OPT0_EXC;
import Imm.ASM.Memory.ASMLdrLabel;
import Imm.ASM.Structural.Label.ASMDataLabel;
import Imm.ASM.Structural.Label.ASMLabel;
import Imm.ASM.Util.Operands.LabelOp;
import Imm.ASM.Util.Operands.RegOp;
import Imm.ASM.Util.REG;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.AST.SyntaxElement;
import Imm.AsN.AsNNode;
import Imm.TYPE.COMPOSIT.INTERFACE;
import Imm.TYPE.COMPOSIT.STRUCT;
import Imm.TYPE.TYPE;
import Opt.AST.ASTOptimizer;
import Snips.CompilerDriver;
import Tools.ASTNodeVisitor;
import Util.MODIFIER;
import Util.NamespacePath;
import Util.Source;
import Util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

	public StructTypedef extension;

	public List<INTERFACE> implemented;

	/** Proviso types provided by the typedef to the extension */
	public List<TYPE> extProviso;

	/* Contains all struct typedefs that extended from this struct */
	public List<StructTypedef> extenders = new ArrayList();

	public STRUCT self;

	private boolean copiedInheritedFunctions = false;

	public HashMap<String, ASMDataLabel> SIDLabelMap = new HashMap();

	public static class StructProvisoMapping {

		private List<TYPE> providedHeadProvisos;

		private List<TYPE> effectiveFieldTypes;

		public HashMap<InterfaceTypedef, ASMLabel> resolverLabelMap = new HashMap();

		public StructProvisoMapping(List<TYPE> providedHeadProvisos, List<TYPE> effectiveFieldTypes) {
			this.providedHeadProvisos = providedHeadProvisos;
			this.effectiveFieldTypes = effectiveFieldTypes;
		}

		public StructProvisoMapping(List<TYPE> providedHeadProvisos, List<TYPE> effectiveFieldTypes, HashMap<InterfaceTypedef, ASMLabel> resolverLabelMap) {
			this.providedHeadProvisos = providedHeadProvisos;
			this.effectiveFieldTypes = effectiveFieldTypes;
			this.resolverLabelMap = resolverLabelMap;
		}

		public List<TYPE> getProvidedProvisos() {
			List<TYPE> clone = new ArrayList();
			for (TYPE t : this.providedHeadProvisos)
				clone.add(t.clone());
			return clone;
		}

		public List<TYPE> getFieldTypes() {
			List<TYPE> clone = new ArrayList();
			for (TYPE t : this.effectiveFieldTypes)
				clone.add(t.clone());
			return clone;
		}

		public StructProvisoMapping clone() {
			return new StructProvisoMapping(this.getProvidedProvisos(), this.getFieldTypes(), this.resolverLabelMap);
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
		/* Add this typedef to extenders of extension */
		if (this.extension != null) {

			/* Notify parent that this struct is extending from it */
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
		}

		if (proviso.isEmpty()) {
			/* Add default proviso mapping if no provisos exist */
			List<TYPE> fieldTypes = new ArrayList();
			for (Declaration d : this.fields)
				fieldTypes.add(d.getType().clone());
			this.registeredMappings.add(new StructProvisoMapping(new ArrayList(), fieldTypes));
		}
	}

	/**
	 * Copy all the functions of the extension to this extension. This needs to be done
	 * when this struct typedef is checked first during context checking. This is due
	 * to the fact that the extension may come from two sources, header and implementation.
	 *
	 * This means that at the point, where {@link #postInitialize()} is called, they have
	 * not been fused yet. So, we have to call it during context checking. Cloning the
	 * function bodies at this moment is not an issue, since they will be re-checked
	 * anyways, so all references will be updated to the new tree.
	 */
	public void copyInheritedFunctions() {
		if (copiedInheritedFunctions) return;
		else copiedInheritedFunctions = true;

		if (this.extension != null) {
			int c = 0;

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
				f0.inheritLink = f;
				f0.body = null;
				f0.path = base;

				/* Apply new source file */
				String file = this.getSource().sourceFile;
				f0.visit(x -> true).forEach(x -> x.getSource().sourceFile = file);

				f0.translateProviso(this.extension.proviso, this.extProviso);

				/* Temporarily disable the provisoFree() part in the isEqualExtended() method */
				STRUCT.useProvisoFreeInCheck = false;

				boolean override = false;
				for (Function fs : this.functions) {
					if (Function.signatureMatch(fs, f0, Function.SIG_M_CRIT.PROVISO_FREE_IN_PARAMS) && this.proviso.size() == this.extension.proviso.size()) {
						override = true;

						/* Set link to actual inherited function */
						fs.inheritLink = f;
					}
				}

				if (!override) {
					this.functions.add(c++, f0);
					this.inheritedFunctions.add(f0);
				}

				STRUCT.useProvisoFreeInCheck = true;
			}
		}

		/* Set reference to struct typedef */
		this.functions.forEach(x -> x.definedInStruct = this);
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
			if (this.fields.get(i).path.equals(path)) {
				/* Copy field and apply field type */
				dec = this.fields.get(i).clone();
				dec.setType(match.effectiveFieldTypes.get(i).clone().provisoFree());
			}
		}
		
		return dec;
	}
	
	public List<Declaration> getFields() {
		return this.fields;
	}
	
	public StructProvisoMapping findMatch(List<TYPE> providedProvisos) {
		
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
			
			if (equal) return m.clone();
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
		for (TYPE type : newActive) ProvisoUtil.mapNTo1(type, clone);
		
		List<TYPE> headMapped = ProvisoUtil.mapToHead(this.proviso, clone);
		
		/* Need to propagate new proviso mapping to extended structs */
		if (this.extension != null) {
			List<TYPE> extTypes = new ArrayList();
			for (TYPE t : this.extProviso) {
				TYPE t0 = t.clone();
				ProvisoUtil.mapNTo1(t0, headMapped);
				extTypes.add(t0.provisoFree());
			}
			
			/* Register mapping at extension */
			this.extension.findMatch(extTypes);
		}
	
		/* Need to propagate new proviso mapping to implemented interfaces */
		for (INTERFACE intf : this.implemented) {
			headMapped = ProvisoUtil.mapToHead(intf.proviso, clone);
			
			List<TYPE> extTypes = new ArrayList();
			for (TYPE t : intf.proviso) {
				TYPE t0 = t.clone();
				ProvisoUtil.mapNTo1(t0, headMapped);
				extTypes.add(t0.provisoFree());
			}
			
			/* Register mapping at implemented interface */
			intf.getTypedef().registerMapping(extTypes);
		}
		
		/* Remove provisos from provided types */
		for (int i = 0; i < clone.size(); i++) 
			clone.set(i, clone.get(i).provisoFree());

		/* Remove provisos from field types */
		for (int i = 0; i < newActive.size(); i++) 
			newActive.set(i, newActive.get(i).provisoFree());
		
		/* Create the new mapping and store it */
		StructProvisoMapping newMapping = new StructProvisoMapping(clone, newActive);
		this.registeredMappings.add(newMapping);
		
		return newMapping;
	}
	
	public void print(int d, boolean rec) {
		String s = Util.pad(d) + "Struct Typedef<" + this.path + ">";
		
		if (this.extension != null)
			s += ":extends:" + this.extension.path + ",";
		
		if (!this.implemented.isEmpty()) {
			if (this.extension != null)
				s += ",";
			else 
				s += ":";
			
			s += "implements:";
		}
		
		for (INTERFACE def : this.implemented)
			s += def.getTypedef().path + ",";
		
		if (this.extension != null || !this.implemented.isEmpty())
			s = s.substring(0, s.length() - 1);
		
		CompilerDriver.outs.println(s);
		
		if (rec) {
			for (Declaration dec : this.fields) 
				dec.print(d + this.printDepthStep, true);
		
			for (Function f : this.functions)
				f.print(d + this.printDepthStep, true);
		}
	}

	public TYPE check(ContextChecker ctx) throws CTEX_EXC {
		ctx.pushTrace(this);
		
		TYPE t = ctx.checkStructTypedef(this);
		
		ctx.popTrace();
		return t;
	}
	
	public SyntaxElement opt(ASTOptimizer opt) throws OPT0_EXC {
		return opt.optStructTypedef(this);
	}
	
	public <T extends SyntaxElement> List<T> visit(ASTNodeVisitor<T> visitor) {
		List<T> result = new ArrayList();
		
		if (visitor.visit(this))
			result.add((T) this);
		
		for (Function f : this.functions)
			result.addAll(f.visit(visitor));
		
		return result;
	}

	public void loadSIDInReg(AsNNode node, REG reg, List<TYPE> context) {
		String postfix = LabelUtil.getProvisoPostfix(context);

		assert this.SIDLabelMap.get(postfix) != null : 
			"Attempted to load SID for a not registered mapping!";
		
		LabelOp operand = new LabelOp(this.SIDLabelMap.get(postfix));
		node.instructions.add(new ASMLdrLabel(new RegOp(reg), operand, null));
	}
	
	public List<String> codePrint(int d) {
		List<String> code = new ArrayList();
		
		String s = "";
		
		if (this.modifier != MODIFIER.SHARED)
			s += this.modifier.toString().toLowerCase() + " ";
		
		s += "struct " + this.path;
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", "<", ">"));
		
		if (!this.implemented.isEmpty() || this.extension != null) {
			s += " : ";
			
			if (this.extension != null) 
				s += this.extension.codePrint(0).get(0) + ", ";
			
			s += this.implemented.stream().map(TYPE::codeString).collect(Collectors.joining(", "));
		}
		
		s += " {";
		
		code.add(Util.pad(d) + s);
		
		for (Declaration d0 : this.fields) 
			code.addAll(d0.codePrint(d + this.printDepthStep));
		
		if (!this.fields.isEmpty() && !this.functions.isEmpty())
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
