package Imm.TYPE.PRIMITIVES;

import java.util.List;

import Exc.SNIPS_EXCEPTION;
import Imm.AST.Function;
import Imm.AST.Statement.Declaration;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;

public class FUNC extends PRIMITIVE<Function> {

	public Function funcHead;
	
	public List<TYPE> proviso;
	
	public FUNC() {

	}
	
	public FUNC(Function funcHead, List<TYPE> proviso) {
		this.funcHead = funcHead;
		this.proviso = proviso;
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type instanceof POINTER) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof FUNC;
		}
		return type instanceof FUNC;
	}
	
	public String typeString() {
		String s = "FUNC<";
		if (this.funcHead != null) {
			for (Declaration dec : this.funcHead.parameters) {
				s += dec.getType().typeString() + ",";
			}
			
			if (!this.funcHead.parameters.isEmpty()) s = s.substring(0, s.length() - 1);
			
			s += " -> ";
			
			s += this.funcHead.getReturnType().typeString();
		}
		else {
			s += "?";
		}
		
		s += ">";
		return s;
	}

	public String sourceCodeRepresentation() {
		throw new SNIPS_EXCEPTION("Cannot get source code representation of FUNC type.");
	}

	public TYPE clone() {
		FUNC b = new FUNC(this.value, this.proviso);
		b.funcHead = this.funcHead;
		return b;
	}

	public void setValue(String value) {
		throw new SNIPS_EXCEPTION("Cannot set value of FUNC type.");
	}
	
}
