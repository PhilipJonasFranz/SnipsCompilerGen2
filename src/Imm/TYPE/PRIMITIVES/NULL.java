package Imm.TYPE.PRIMITIVES;

import Exc.SNIPS_EXC;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Res.Const;

public class NULL extends PRIMITIVE<Object> {
	
			/* --- METHODS --- */
	public void setValue(String value) {
		
	}
	
	public boolean isEqual(TYPE type) {
		return type instanceof POINTER || type instanceof NULL;
	}
	
	public String typeString() {
		return "NULL";
	}
	
	public String sourceCodeRepresentation() {
		throw new SNIPS_EXC(Const.CANNOT_GET_SOURCE_CODE_REPRESENTATION, this.typeString());
	}
	
	public int wordsize() {
		return 1;
	}
	
	public TYPE getCoreType() {
		return this;
	}

	public NULL clone() {
		return new NULL();
	}

} 
