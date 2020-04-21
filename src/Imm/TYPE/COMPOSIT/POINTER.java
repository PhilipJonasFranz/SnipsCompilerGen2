package Imm.TYPE.COMPOSIT;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;
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
				/* Compare Struct Names and provided provisos */
				STRUCT s0 = (STRUCT) this.getCoreType();
				STRUCT s1 = (STRUCT) pointer.getCoreType();
				
				if (!s0.proviso.isEmpty()) {
					if (s0.proviso.size() != s1.proviso.size()) return false;
					else {
						for (int i = 0; i < s0.proviso.size(); i++) {
							if (!s0.proviso.get(i).isEqual(s1.proviso.get(i))) {
								return false;
							}
						}
					}
				}
				
				return s0.typedef.structName.equals(s1.typedef.structName);
			}
			else if (pointer.getCoreType() instanceof STRUCT || this.getCoreType() instanceof STRUCT) {
				/* Only one of both is struct, return false */
				return false;
			}
			else {
				return this.coreType.isEqual(pointer.coreType);
			}
		}
		else if (type instanceof PROVISO) {
			PROVISO p = (PROVISO) type;
			if (p.hasContext()) return this.isEqual(p.getContext());
			else return false;
		}
		else {
			/* Compare Core Types */
			return this.getCoreType().isEqual(type.getCoreType());
		}
	}
	
	public String typeString() {
		if (this.targetType instanceof STRUCT) {
			STRUCT s = (STRUCT) this.targetType;
			String t = ((s.typedef != null)? s.typedef.structName : "?") + "*";
			if (CompilerDriver.printProvisoTypes) t += s.getProvisoString();
			return t;
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
			/* 
			 * Cannot clone the struct pointer since it may be recursive, but can guarantee that
			 * it can point to itself.
			 */
			return new POINTER(this.targetType);
		}
		else {
			POINTER p = new POINTER(this.targetType.clone());
			
			/* Make sure cloned type is equal to this type */
			assert (p.typeString().equals(this.typeString()));
			
			return p;
		}
	}

}
