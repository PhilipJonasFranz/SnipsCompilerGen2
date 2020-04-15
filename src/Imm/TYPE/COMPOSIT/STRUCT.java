package Imm.TYPE.COMPOSIT;

import java.util.List;

import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.TYPE;

public class STRUCT extends COMPOSIT {

	public StructTypedef typedef;
	
	public List<Declaration> fields;
	
	public STRUCT(StructTypedef typedef) {
		super(null);
		this.typedef = typedef;
		this.fields = typedef.declarations;
	}
	
	public boolean isEqual(TYPE type) {
		if (type instanceof STRUCT) {
			STRUCT struct = (STRUCT) type;
			if (struct.fields.size() == this.fields.size()) {
				boolean isEqual = true;
				for (int i = 0; i < this.fields.size(); i++) {
					isEqual &= this.fields.get(i).type.isEqual(struct.fields.get(i).type);
				}
				return isEqual;
			}
		}
		
		return false;
	}
	
	public Declaration getField(String name) {
		for (Declaration dec : this.fields) {
			if (dec.fieldName.equals(name)) {
				return dec;
			}
		}
		return null;
	}
	
	public int getFieldByteOffset(String name) {
		int offset = 0;
		for (Declaration dec : this.fields) {
			if (dec.fieldName.equals(name)) {
				return offset * 4;
			}
			else offset += dec.type.wordsize();
		}
		return -1;
	}
	
	public boolean hasField(String name) {
		for (Declaration dec : this.fields) {
			if (dec.fieldName.equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	public String typeString() {
		String s = this.typedef.structName + "<";
		for (Declaration t : fields) {
			s += t.type.typeString() + ",";
		}
		s = s.substring(0, s.length() - 1);
		s += ">";
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
		for (Declaration dec : this.fields) {
			sum += dec.type.wordsize();
		}
		return sum;
	}

	public TYPE getCoreType() {
		/* Struct acts as its own type, so its is own core type. */
		return this;
	}
	
}
