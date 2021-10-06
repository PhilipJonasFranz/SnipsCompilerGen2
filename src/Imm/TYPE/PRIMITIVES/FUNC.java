package Imm.TYPE.PRIMITIVES;

import java.util.List;
import java.util.stream.Collectors;

import Exc.CTEX_EXC;
import Exc.SNIPS_EXC;
import Imm.AST.Function;
import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.COMPOSIT.POINTER;
import Res.Const;
import Util.Source;

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
		if (type.getCoreType().isVoid()) return true;
		if (type.isProviso()) {
			PROVISO p = (PROVISO) type;
			return p.isEqual(this);
		}
		else if (type.isPointer()) {
			POINTER p = (POINTER) type;
			return p.getCoreType() instanceof FUNC;
		}
		else if (type instanceof FUNC) {
			FUNC f0 = (FUNC) type;
			boolean equal = true;
			
			if (f0.funcHead != null && this.funcHead != null) {
				if (f0.funcHead.parameters.size() != this.funcHead.parameters.size()) equal = false;
				else {
					for (int i = 0; i < this.funcHead.parameters.size(); i++) equal &= f0.funcHead.parameters.get(i).getType().isEqual(this.funcHead.parameters.get(i).getType());
					equal &= this.funcHead.getReturnTypeDirect().isEqual(f0.funcHead.getReturnTypeDirect());
				}
			}
			
			return equal;
			
		}
		else return false;
	}
	
	public CTEX_EXC getInequality(FUNC func, Source source) {
		if (func.funcHead.parameters.size() != this.funcHead.parameters.size()) return new CTEX_EXC(source, Const.MISSMATCHING_ARGUMENT_NUMBER, this.funcHead.parameters.size(), func.funcHead.parameters.size());
		else {
			for (int i = 0; i < this.funcHead.parameters.size(); i++) {
				if (!func.funcHead.parameters.get(i).getType().isEqual(this.funcHead.parameters.get(i).getType())) {
					return new CTEX_EXC(source, Const.PARAMETER_TYPE_DOES_NOT_MATCH, func.funcHead.parameters.get(i).getType().typeString(), this.funcHead.parameters.get(i).getType().typeString());
				}
				
			}
			
			return new CTEX_EXC(source, Const.RETURN_TYPE_DOES_NOT_MATCH, func.funcHead.getReturnType().typeString(), this.funcHead.getReturnType().typeString());
		}
	}
	
	public String typeString() {
		String s = "FUNC<";
		if (this.funcHead != null) {
			s += this.funcHead.parameters.stream().map(x -> x.getType().toString()).collect(Collectors.joining(","));
			s += " -> ";
			s += this.funcHead.getReturnTypeDirect().typeString();
		}
		else s += "?";
		
		s += ">";
		return s;
	}

	public String sourceCodeRepresentation() {
		throw new SNIPS_EXC(Const.CANNOT_GET_SOURCE_CODE_REPRESENTATION, this.typeString());
	}

	public TYPE clone() {
		FUNC b = new FUNC(this.value, this.proviso);
		b.funcHead = this.funcHead;
		return b;
	}

	public void setValue(String value) {
		throw new SNIPS_EXC(Const.CANNOT_SET_VALUE_OF_TYPE, this.typeString());
	}
	
	public String codeString() {
		// TODO: Add signature if available
		return "func";
	}
	
} 
