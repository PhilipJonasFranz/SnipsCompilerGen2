package Imm.TYPE.COMPOSIT;

import java.util.List;

import Imm.TYPE.TYPE;

public class STRUCT extends COMPOSIT {

	List<TYPE> types;
	
	public STRUCT(String value, List<TYPE> types) {
		super(value);
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
	
	public String typeString() {
		String s = "STRUCT<";
		for (TYPE t : types) {
			s += t.typeString() + ",";
		}
		s = s.substring(0, s.length() - 1);
		s += ">";
		return s;
	}

	@Override
	public void setValue(String value) {
		return;
	}

	@Override
	public String sourceCodeRepresentation() {
		return null;
	}

	@Override
	public int wordsize() {
		int sum = 0;
		for (TYPE type : this.types) {
			sum += type.wordsize();
		}
		return sum;
	}
	
}
