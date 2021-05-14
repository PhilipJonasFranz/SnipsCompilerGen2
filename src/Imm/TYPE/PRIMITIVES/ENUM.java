package Imm.TYPE.PRIMITIVES;

import Imm.AST.Typedef.EnumTypedef;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;

public class ENUM extends PRIMITIVE<String> {

	public EnumTypedef def;
	
	public String fieldName;
	
	public ENUM(EnumTypedef def, String fieldName, int mapping) {
		super("" + mapping);
		this.fieldName = fieldName;
		this.def = def;
	}
	
	public void setValue(String value) {
		this.value = value;
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type.isPointer()) {
			POINTER p = (POINTER) type;
			if (p.getCoreType() instanceof ENUM e0) {
				return e0.def.equals(this.def);
			}
			else return false;
		}
		
		if (type instanceof ENUM e0) {
			return e0.def.equals(this.def);
		}
		else return false;
	}
	
	public String typeString() {
		return "ENUM<" + this.def.path + ">";
	}

	public String sourceCodeRepresentation() {
		/* 
		 * Return the enum field mapping, that enum is part of the
		 * enum type is already checked.
		 */
		return this.value;
	}

	public TYPE clone() {
		ENUM b = new ENUM(this.def, this.fieldName, Integer.parseInt(this.getValue()));
		if (this.value != null) b.setValue(this.value + "");
		return b;
	}
	
	public String codeString() {
		return this.def.path.build();
	}
	
} 
