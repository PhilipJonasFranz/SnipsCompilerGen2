package Imm.TYPE;

import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Par.Token;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public abstract class TYPE<T> {

			/* --- FIELDS --- */
	protected int wordSize = 1;
	
	@Getter
	public T value;
	
	
			/* --- CONSTRUCTORS --- */
	public TYPE(String initialValue) {
		this.setValue(initialValue);
	}
	
	
			/* --- METHODS --- */
	public abstract void setValue(String value);
	
	public abstract boolean isEqual(TYPE type);
	
	public boolean hasValue() {
		return this.value != null;
	}
	
	public abstract String typeString();
	
	public abstract String sourceCodeRepresentation();
	
	public abstract int wordsize();
	
	public abstract TYPE getCoreType();
	
	public static TYPE fromToken(Token token) {
		TYPE t = PRIMITIVE.fromToken(token);
		if (t == null) return new PROVISO(token.spelling);
		else return t;
	}
	
	public abstract TYPE clone();
	
}
