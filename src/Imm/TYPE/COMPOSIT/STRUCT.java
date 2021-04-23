package Imm.TYPE.COMPOSIT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import Exc.CTEX_EXC;
import Imm.AST.Statement.Declaration;
import Imm.AST.Typedef.StructTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Res.Const;
import Snips.CompilerDriver;
import Util.NamespacePath;
import Util.Source;

public class STRUCT extends COMPOSIT {

	public static boolean useProvisoFreeInCheck = true;
	
	private StructTypedef typedef;
	
	public List<TYPE> proviso;
	
	public STRUCT(StructTypedef typedef, List<TYPE> proviso) {
		super(null);
		this.typedef = typedef;
		this.proviso = proviso;
	}
	
	public boolean isEqual(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isInterface()) {
			INTERFACE i = (INTERFACE) type;
			for (INTERFACE def : this.typedef.implemented)
				if (i.getTypedef().equals(def.getTypedef())) {
					return true;
				}
			
			return false;
		}
		if (type.isStruct()) {
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
	 * The passed types acts in this case as the child of this type.
	 */
	public boolean isEqualExtended(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isStruct()) {
			STRUCT struct = (STRUCT) type;
			
			StructTypedef sDef = this.typedef;
			while (!sDef.equals(struct.typedef)) {
				if (sDef.extension == null) 
					return false;
				else sDef = sDef.extension;
			}
			
			if (sDef.getFields().size() == struct.typedef.getFields().size() && struct.proviso.size() == this.proviso.size()) {
				boolean isEqual = true;
				
				/* Compare Provisos, rest of subtree must be equal */
				for (int i = 0; i < this.proviso.size(); i++) {
					if (useProvisoFreeInCheck) 
						isEqual &= this.proviso.get(i).provisoFree().isEqual(struct.proviso.get(i).provisoFree());
					else 
						isEqual &= this.proviso.get(i).isEqual(struct.proviso.get(i));
				}
				
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
		if (t.isStruct()) {
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
	
	public void checkProvisoPresent(Source source) throws CTEX_EXC {
		if (this.proviso.size() != this.typedef.proviso.size())
			throw new CTEX_EXC(source, Const.MISSMATCHING_NUMBER_OF_PROVISOS, this.typedef.proviso.size(), this.proviso.size());
	}
	
	/**
	 * Returns the amount of bytes the field that corresponds to the given namespace
	 * path is offsetted from the start of the structure. For example, the first field
	 * would be offsetted 0 bytes or 4, if SIDs are enabled. If this field is 8 bytes large,
	 * the second field is offsetted 8 bytes or 12 bytes, respectiveley. Returns -1 if no field
	 * matches the given namespace path.
	 * @param path The namespace path that should match the field.
	 * @return The amount of bytes the field is offsetted or -1.
	 */
	public int getFieldByteOffset(NamespacePath path) {
		int offset = (!CompilerDriver.disableStructSIDHeaders)? 1 : 0;
		
		for (int i = 0; i < this.typedef.getFields().size(); i++) {
			Declaration dec = this.getField(this.typedef.getFields().get(i).path);
			
			if (dec.path.equals(path)) {
				return offset * 4;
			}
			else offset += dec.getType().wordsize();
		}
	
		return -1;
	}
	
	public String typeString() {
		String s = this.typedef.path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(",", "<", ">"));
		
		if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
		return s;
	}
	
	public String getProvisoString() {
		String s = "";
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", " {", "}"));
		
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
	
	public TYPE getContainedType() {
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
		for (int i = 0; i < this.proviso.size(); i++) 
			this.proviso.set(i, this.proviso.get(i).remapProvisoName(name, newType));
		
		return this;
	}

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType.isStruct()) {
			STRUCT s = (STRUCT) mapType;
			if (s.isPolymorphTo(this)) {
				/* Missing provisos */
				if (this.proviso.size() != s.proviso.size())
					return null;
				
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

	public boolean hasProviso() {
		for (TYPE t : this.proviso)
			if (t.hasProviso())
				return true;
		return false;
	}
	
	public String codeString() {
		String s = this.getTypedef().path.build();
		
		if (!this.proviso.isEmpty()) 
			s += this.proviso.stream().map(TYPE::toString).collect(Collectors.joining(", ", "<", ">"));
		
		return s;
	}
	
} 
