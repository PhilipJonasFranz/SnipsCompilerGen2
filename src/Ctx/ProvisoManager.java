package Ctx;

import java.util.ArrayList;
import java.util.List;

import CGen.LabelGen;
import Exc.CTX_EXCEPTION;
import Exc.SNIPS_EXCEPTION;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.ARRAY;
import Imm.TYPE.COMPOSIT.POINTER;
import Imm.TYPE.COMPOSIT.STRUCT;
import Util.Pair;
import Util.Source;

public class ProvisoManager {
	
			/* --- FIELDS --- */
	/** List of the provisos types this function is templated with */
	public List<TYPE> provisosTypes;
	
	/** A list that contains the combinations of types this function was templated with */
	public List<Pair<String, Pair<TYPE, List<TYPE>>>> provisosCalls = new ArrayList();
	
	private Source source;
	
	public ProvisoManager(Source source, List<TYPE> provisoTypes) {
		this.provisosTypes = provisoTypes;
		this.source = source;
	}
	
	public void printContexts() {
		System.out.println("Mappings: ");
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			System.out.print(this.provisosCalls.get(i).second.first.typeString() + " : ");
			List<TYPE> map = this.provisosCalls.get(i).second.second;
			for (TYPE t : map) System.out.print(t.typeString() + ", ");
			System.out.println();
		}
	}
	
	public void setContext(List<TYPE> context) throws CTX_EXCEPTION {
		if (context.size() != this.provisosTypes.size()) {
			throw new CTX_EXCEPTION(source, "Missmatching number of provided provisos, expected " + this.provisosTypes.size() + ", got " + context.size());
		}
		
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			TYPE pro = this.provisosTypes.get(i);
			if (!(pro instanceof PROVISO)) {
				throw new CTX_EXCEPTION(source, "Provided Type " + pro.typeString() + " is not a proviso type");
			}
			
			PROVISO pro0 = (PROVISO) pro;
			pro0.setContext(context.get(i));
		}
	}
	
	public boolean isActiveContext(List<TYPE> context) {
		return this.mappingIsEqual(context, this.provisosTypes);
	}
	
	public void releaseContext() {
		for (int i = 0; i < this.provisosTypes.size(); i++) {
			PROVISO pro0 = (PROVISO) this.provisosTypes.get(i);
			pro0.releaseContext();
		}
	}
	
	public boolean containsMapping(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			if (this.mappingIsEqual(map0, map)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean mappingIsEqual(List<TYPE> map0, List<TYPE> map1) {
		boolean isEqual = true;
		for (int a = 0; a < map0.size(); a++) {
			isEqual &= map0.get(a).isEqual(map1.get(a));
		}
		return isEqual;
	}
	
	public String getPostfix(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			if (this.mappingIsEqual(map0, map)) {
				return this.provisosCalls.get(i).first;
			}
		}
		
		return null;
	}
	
	public TYPE getMappingReturnType(List<TYPE> map) {
		for (int i = 0; i < this.provisosCalls.size(); i++) {
			List<TYPE> map0 = this.provisosCalls.get(i).second.second;
			
			boolean isEqual = true;
			for (int a = 0; a < map0.size(); a++) {
				isEqual &= map0.get(a).isEqual(map.get(a));
			}
			
			if (isEqual) return this.provisosCalls.get(i).second.first;
		}
		
		throw new SNIPS_EXCEPTION("No mapping!");
	}
	
	public void addProvisoMapping(TYPE type, List<TYPE> context) {
		if (this.containsMapping(context)) {
			return;
		}
		else {
			String postfix = (context.isEmpty())? "" : LabelGen.getProvisoPostfix();
			this.provisosCalls.add(new Pair<String, Pair<TYPE, List<TYPE>>>(postfix, new Pair<TYPE, List<TYPE>>(type, context)));
		}
	}
	
	public static void setContext(List<TYPE> context, TYPE type) throws CTX_EXCEPTION {
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			for (TYPE t : context) {
				if (p.isEqual(t)) {
					p.setContext(t.clone());
					break;
				}
			}
		}
		else if (type instanceof ARRAY) {
			ARRAY arr = (ARRAY) type;
			setContext(context, arr.elementType);
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			setContext(context, p.targetType);
		}
		else if (type instanceof STRUCT) {
			STRUCT s = (STRUCT) type;
			
			List<TYPE> clone = new ArrayList();
			for (TYPE t : context) clone.add(t.clone());
			mapContextTo(s.proviso, clone);
			
			for (Declaration d : s.typedef.fields) {
				/* Prevent Recursion */
				if (!(d.getType() instanceof POINTER)) {
					setContext(clone, d.getType());
					
					if (d.getType() instanceof STRUCT) {
						STRUCT s0 = (STRUCT) d.getType();
						s0 = (STRUCT) setHiddenContext(s0);
					}
				}
			}
		}
	}
	
	/**
	 * Maps the proviso types of the second argument to the first.
	 */
	public static void mapContextTo(List<TYPE> target, List<TYPE> source) {
		for (int i = 0; i < target.size(); i++) {
			for (int a = 0; a < source.size(); a++) {
				if (target.get(i) instanceof PROVISO && target.get(i).isEqual(source.get(a))) {
					PROVISO p = (PROVISO) target.get(i);
					p.setContext(source.get(a).clone());
				}
			}
		}
	}
	
	public static TYPE setHiddenContext(TYPE type) throws CTX_EXCEPTION {
		if (type instanceof PROVISO) {
			return type;
		}
		else if (type instanceof ARRAY) {
			ARRAY arr = (ARRAY) type;
			arr.elementType = setHiddenContext(arr.elementType);
			return arr;
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			p.targetType = setHiddenContext(p.targetType);
			return p;
		}
		else if (type instanceof STRUCT) {
			STRUCT s = (STRUCT) type;
			
			/* Map initialization proviso types to proviso head listing */
			for (int i = 0; i < s.typedef.proviso.size(); i++) {
				PROVISO p = (PROVISO) s.typedef.proviso.get(i);
				p.setContext(s.proviso.get(i));
			}
			
			/* Propagate initialized proviso mapping on the fields */
			ProvisoManager.setContext(s.typedef.proviso, s);
			
			return s;
		}
		else return type;
	}
	
}
