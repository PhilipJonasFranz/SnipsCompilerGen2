package Imm.TYPE.COMPOSIT;

import java.util.List;

import Imm.TYPE.TYPE;

public class STRUCT extends TYPE {

	List<TYPE> types;
	
	public STRUCT(List<TYPE> types) {
		this.types = types;
	}

	public boolean isEqual(TYPE type) {
		if (type instanceof STRUCT) {
			STRUCT struct = (STRUCT) type;
			if (struct.types.size() == this.types.size()) {
				boolean isEqual = true;
				for (int i = 0; i < this.types.size(); i++) {
					isEqual &= this.types.get(i).isEqual(struct.types.get(i));
				}
				return isEqual;
			}
		}
		
		return false;
	}
	
}
