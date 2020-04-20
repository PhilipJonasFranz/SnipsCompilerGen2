package Imm.TYPE.COMPOSIT;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import lombok.Getter;

public class POINTER extends COMPOSIT {

	/** The type that this pointer capsules. */
	public TYPE targetType;

	@Getter
	public TYPE coreType;
	
	public POINTER(TYPE targetType) {
		super(null);
		this.targetType = targetType;
		
		if (targetType instanceof PRIMITIVE) {
			this.coreType = targetType;
		}
		else if (targetType instanceof COMPOSIT) {
			this.coreType = ((COMPOSIT) targetType).getCoreType();
		}
	}

	public boolean isEqual(TYPE type) {
		if (type.getCoreType() instanceof VOID) return true;
		if (type instanceof POINTER) {
			POINTER pointer = (POINTER) type;
			if (pointer.getCoreType() instanceof STRUCT && this.getCoreType() instanceof STRUCT) {
				STRUCT s0 = (STRUCT) this.getCoreType();
				STRUCT s1 = (STRUCT) pointer.getCoreType();
				
				if (!s0.proviso.isEmpty()) {
					if (s0.proviso.size() != s1.proviso.size()) return false;
					else {
						for (int i = 0; i < s0.proviso.size(); i++) {
							if (!s0.proviso.get(i).isEqual(s1.proviso.get(i))) {
								System.out.println("Proviso not equal: " + s0.proviso.get(i).typeString() + " " + s1.proviso.get(i).typeString());
								return false;
							}
						}
					}
				}
				
				return s0.typedef.structName.equals(s1.typedef.structName);
			}
			else if (pointer.getCoreType() instanceof STRUCT || this.getCoreType() instanceof STRUCT) {
				return false;
			}
			else return this.coreType.isEqual(pointer.coreType);
		}
		else if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			if (p.hasContext()) return this.isEqual(p.getContext());
			else return false;
		}
		else return false;
	}
	
	public String typeString() {
		if (this.targetType instanceof STRUCT) {
			STRUCT s = (STRUCT) this.targetType;
			return "STRUCT<" + ((s.typedef != null)? s.typedef.structName : "?") + ">*";
		}
		else return this.targetType.typeString() + "*";
	}

	public void setValue(String value) {
		return;
	}

	public String sourceCodeRepresentation() {
		return null;
	}

	public int wordsize() {
		return 1;
	}

	public TYPE clone() {
		if (this.targetType instanceof STRUCT) {
			POINTER p = new POINTER(this.targetType);
			p.coreType = this.coreType;
			return p;
		}
		else {
			TYPE target = this.targetType.clone();
			POINTER p = new POINTER(target);
			p.coreType = this.coreType.clone();
			return p;
		}
	}

}
