package Imm.TYPE.COMPOSIT;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.NULL;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Imm.TYPE.PRIMITIVES.VOID;
import Snips.CompilerDriver;

public class POINTER extends COMPOSIT {

	/** The type that this pointer capsules. */
	public TYPE targetType;

	public TYPE coreType;
	
	public POINTER(TYPE targetType) {
		super(null);
		this.targetType = targetType;
		this.coreType = this.targetType.getCoreType();
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof NULL) return true;
		if (type.getCoreType() instanceof VOID || this.getCoreType() instanceof VOID) return true;
		if (type instanceof POINTER) {
			POINTER pointer = (POINTER) type;
			if (pointer.targetType instanceof STRUCT && this.targetType instanceof STRUCT) {
				STRUCT s = (STRUCT) this.targetType;
				return s.isEqualExtended(type);
			}
			else return this.targetType.isEqual(pointer.targetType);
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
			String t = ((s.getTypedef() != null)? s.typeString() : "?") + "*";
			if (CompilerDriver.printProvisoTypes) t += s.getProvisoString();
			if (CompilerDriver.printObjectIDs) t += " " + this.toString().split("@") [1];
			return t;
		}
		else {
			String s = this.targetType.typeString() + "*";
			if (CompilerDriver.printObjectIDs) s += " " + this.toString().split("@") [1];
			return s;
		}
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
			return new POINTER(this.targetType.clone());
		}
		else {
			POINTER p = new POINTER(this.targetType.clone());
			
			/* Make sure cloned type is equal to this type */
			assert (p.typeString().equals(this.typeString()));
			
			return p;
		}
	}

	public TYPE getCoreType() {
		if (targetType instanceof PRIMITIVE) {
			this.coreType = targetType;
		}
		else if (targetType instanceof COMPOSIT) {
			this.coreType = ((COMPOSIT) targetType).getCoreType();
		}
		else if (this.targetType instanceof PROVISO) {
			this.coreType = this.targetType.getCoreType();
		}
		return this.coreType;
	}

	public TYPE provisoFree() {
		POINTER p = (POINTER) this.clone();
		p.targetType = p.targetType.provisoFree();
		p.coreType = p.coreType.provisoFree();
		return p;
	}

	public TYPE remapProvisoName(String name, TYPE newType) {
		this.targetType = this.targetType.remapProvisoName(name, newType);
		return this;
	}

} 
