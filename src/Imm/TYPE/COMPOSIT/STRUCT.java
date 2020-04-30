package Imm.TYPE.COMPOSIT;

import java.util.ArrayList;
import java.util.List;

import Imm.AST.Statement.Declaration;
import Imm.AST.Statement.StructTypedef;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;
import Util.NamespacePath;

public class STRUCT extends COMPOSIT {

	public StructTypedef typedef;
	
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
			
			if (struct.typedef.fields.size() == this.typedef.fields.size()) {
				boolean isEqual = true;
				for (int i = 0; i < this.typedef.fields.size(); i++) {
					isEqual &= this.typedef.fields.get(i).getType().isEqual(struct.typedef.fields.get(i).getType());
				}
				return isEqual;
			}
			
		}
		
		return false;
	}
	
	public Declaration getField(NamespacePath path) {
		for (Declaration dec : this.typedef.fields) {
			if (dec.path.build().equals(path.build())) {
				return dec;
			}
		}
		return null;
	}
	
	public int getFieldByteOffset(NamespacePath path) {
		int offset = 0;
		for (Declaration dec : this.typedef.fields) {
			if (dec.path.build().equals(path.build())) {
				return offset * 4;
			}
			else offset += dec.getType().wordsize();
		}
		return -1;
	}
	
	public boolean hasField(NamespacePath path) {
		for (Declaration dec : this.typedef.fields) {
			if (dec.path.build().equals(path.build())) {
				return true;
			}
		}
		return false;
	}
	
	public String typeString() {
		String s = this.typedef.path.build();
		
		if (this.typedef.fields.size() > 0) {
			s += "<";
			for (Declaration t : this.typedef.fields) {
				/* Field is recursive type, cast to struct and print only name and proviso */
				if (t.getType().getCoreType().isEqual(this) && !(t.getType().getCoreType() instanceof VOID)) {
					STRUCT s0 = (STRUCT) t.getType().getCoreType();
					
					s += this.typedef.path.build();
					
					if (CompilerDriver.printProvisoTypes) s += s0.getProvisoString();
					if (CompilerDriver.printObjectIDs) s += " " + s0.toString().split("@") [1];
					s += ",";
				}
				else s += t.getType().typeString() + ",";
			}
			s = s.substring(0, s.length() - 1);
			s += ">";
		}
		
		if (CompilerDriver.printProvisoTypes) s += this.getProvisoString();
		
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
		for (Declaration dec : this.typedef.fields) {
			sum += dec.getType().wordsize();
		}
		return sum;
	}

	public TYPE getCoreType() {
		/* Struct acts as its own type, so its is own core type. */
		return this;
	}

	public STRUCT clone() {
		List<TYPE> prov0 = new ArrayList();
		for (TYPE t : this.proviso) prov0.add(t.clone());
		STRUCT s = new STRUCT(prov0);
		
		s.typedef = this.typedef.clone();
		
		return s;
	}
	
}
