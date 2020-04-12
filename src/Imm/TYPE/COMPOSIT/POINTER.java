package Imm.TYPE.COMPOSIT;

import Imm.TYPE.TYPE;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
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
		if (type instanceof POINTER) {
			POINTER pointer = (POINTER) type;
			return this.coreType.isEqual(pointer.coreType);
		}
		else return false;
	}
	
	public String typeString() {
		return this.targetType.typeString() + "*";
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

}
