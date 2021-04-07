package Imm.TYPE.COMPOSIT;

import Imm.TYPE.PROVISO;
import Imm.TYPE.TYPE;
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
		if (type.isNull()) return true;
		if (type.getCoreType().isVoid() || this.getCoreType().isVoid()) return true;
		if (type.isPointer()) {
			POINTER pointer = (POINTER) type;
			if (pointer.targetType.isStruct() && this.targetType.isStruct()) {
				STRUCT s = (STRUCT) this.targetType;
				return s.isEqualExtended(pointer.targetType);
			}
			else return this.targetType.isEqual(pointer.targetType);
		}
		else if (type.isProviso()) {
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
		if (this.targetType.isStruct()) {
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
		if (this.targetType.isStruct()) {
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
		if (targetType.isPrimitive()) {
			this.coreType = targetType;
		}
		else if (targetType.isComposit()) {
			this.coreType = ((COMPOSIT) targetType).getCoreType();
		}
		else if (this.targetType.isProviso()) {
			this.coreType = this.targetType.getCoreType();
		}
		return this.coreType;
	}
	
	public TYPE getContainedType() {
		return this.targetType;
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

	public TYPE mappable(TYPE mapType, String searchedProviso) {
		if (mapType.isPointer()) {
			POINTER p = (POINTER) mapType;
			return this.targetType.mappable(p.targetType, searchedProviso);
		}
		else return null;
	}

	public boolean hasProviso() {
		return this.targetType.hasProviso();
	}
	
	public String codeString() {
		return this.targetType.codeString() + "*";
	}

} 
