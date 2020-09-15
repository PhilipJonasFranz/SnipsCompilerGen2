package Imm.TYPE.COMPOSIT;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;
import Util.NamespacePath;

public class STRUCT extends COMPOSIT {

	private StructTypedef typedef;
	
	public List<TYPE> proviso;
	
	public STRUCT(StructTypedef typedef, List<TYPE> proviso) {
		super(null);
		this.typedef = typedef;
		this.proviso = proviso;
	}
	
	public STRUCT(List<TYPE> proviso) {
		super(null);
		this.proviso = proviso;
	}
	
	public boolean isEqual(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof STRUCT) {
			STRUCT struct = (STRUCT) type;
			
			StructTypedef sDef = struct.typedef;
			if (sDef.getFields().size() == this.typedef.getFields().size() && struct.proviso.size() == this.proviso.size()) {
				boolean isEqual = true;
				
				/* Compare Provisos, rest of subtree must be equal */
				for (int i = 0; i < this.proviso.size(); i++) 
					isEqual &= this.proviso.get(i).isEqual(struct.proviso.get(i));
				
				return isEqual;
			}
			
		}
		
		return false;
	}
	
	/**
	 * Check if the types are equal if struct extending is taken into consideration.
	 */
	public boolean isEqualExtended(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof STRUCT) {
			STRUCT struct = (STRUCT) type;
			
			StructTypedef sDef = struct.typedef;
			while (!sDef.equals(this.typedef)) {
				if (sDef.extension == null) 
					return false;
				else sDef = sDef.extension;
			}
			
			if (sDef.getFields().size() == this.typedef.getFields().size() && struct.proviso.size() == this.proviso.size()) {
				boolean isEqual = true;
				
				/* Compare Provisos, rest of subtree must be equal */
				for (int i = 0; i < this.proviso.size(); i++) 
					isEqual &= this.proviso.get(i).provisoFree().isEqual(struct.proviso.get(i).provisoFree());
				
				return isEqual;
			}
			
		}
		
		return false;
	}
	
	/**
	 * Check if this type extends from given type. Returns only
	 * true if given type is a struct and this struct extends from it.
	 */
	public boolean isPolymorphTo(TYPE t) {
		if (t instanceof STRUCT) {
			STRUCT s = (STRUCT) t;
			
			StructTypedef sDef = this.typedef;
			while (!sDef.equals(s.getTypedef())) {
				if (sDef.extension == null) 
					return false;
				else sDef = sDef.extension;
			}
			
			return true;
		}
		
		return false;
	}
	
	public StructTypedef getTypedef() {
		return this.typedef;
	}
	
	public Declaration getField(NamespacePath path) {
		/* Relay to typedef requestField(), but use own proviso as key */
		return this.typedef.requestField(path, this.proviso);
	}
	
	public int getNumberOfFields() {
		return this.typedef.getFields().size();
	}
	
	public Declaration getFieldNumber(int i) {
		return this.getField(this.typedef.getFields().get(i).path);
	}
	
	public Declaration getFieldNumberDirect(int i) {
		return this.typedef.getFields().get(i).clone();
	}
	
	public int getFieldByteOffset(NamespacePath path) {
		int offset = (!CompilerDriver.disableStructSIDHeaders)? 1 : 0;
		
		for (int i = 0; i < this.typedef.getFields().size(); i++) {
			Declaration dec = this.getField(this.typedef.getFields().get(i).path);
			
			if (dec.path.build().equals(path.build())) {
				return offset * 4;
			}
			else offset += dec.getType().wordsize();
		}
	
		return -1;
	}
	
	public boolean hasField(NamespacePath path) {
		for (Declaration dec : this.typedef.getFields()) 
			if (dec.path.build().equals(path.build())) return true;
		
		return false;
	}
	
	public String typeString() {
		String s = this.typedef.path.build();
		
		if (!this.proviso.isEmpty()) {
			s += "<";
			for (TYPE t : this.proviso) {
				s += t.typeString() + ",";
			}
			s = s.substring(0, s.length() - 1);
			s += ">";
		}
		
		if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
		return s;
	}
	
	public String getProvisoString() {
		String s = "";
		if (!this.proviso.isEmpty()) {
			s += " {";
			for (TYPE t : this.proviso) s += t.typeString() + ", ";
			s = s.substring(0, s.length() - 2);
			s += "}";
		}
		return s;
	}

	public void setValue(String value) {
		return;
	}

	public String sourceCodeRepresentation() {
		return null;
	}

	public int wordsize() {
		int sum = 0;
		for (int i = 0; i < this.typedef.getFields().size(); i++) { 
			Declaration dec = this.getField(this.typedef.getFields().get(i).path);
			sum += dec.getType().wordsize();
		}
		
		return sum + ((!CompilerDriver.disableStructSIDHeaders)? 1 : 0);
	}

	public TYPE getCoreType() {
		/* Struct acts as its own type, so its is own core type. */
		return this;
	}

	public STRUCT clone() {
		List<TYPE> prov0 = new ArrayList();
		
		for (TYPE t : this.proviso) 
			prov0.add(t.clone());
		
		return new STRUCT(this.typedef, prov0);
	}

	public TYPE provisoFree() {
		STRUCT s = (STRUCT) this.clone();
		for (int i = 0; i < s.proviso.size(); i++) s.proviso.set(i, s.proviso.get(i).provisoFree());
		return s;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		for (int i = 0; i < this.proviso.size(); i++) { 
			this.proviso.set(i, this.proviso.get(i).remapProvisoName(name, newType));
		}
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType instanceof STRUCT) {
			STRUCT s = (STRUCT) mapType;
			if (s.getTypedef().SID == this.getTypedef().SID) {
				for (int i = 0; i < this.proviso.size(); i++) {
					PROVISO prov = (PROVISO) this.proviso.get(i);
					if (prov.placeholderName.equals(searchedProviso)) {
						return s.proviso.get(i);
					}
						
				}
			}
		}
		
		return null;
	}
	
} 
