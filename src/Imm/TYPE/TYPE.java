package Imm.TYPE;

import Imm.TYPE.PRIMITIVES.BOOL;
import Imm.TYPE.PRIMITIVES.INT;
import Par.Token;
import Par.Token.TokenType;

public abstract class TYPE<T> {

			/* --- FIELDS --- */
	public int wordSize = 1;
	
	public T value;
	
	
			/* --- CONSTRUCTORS --- */
	public TYPE() {
		
	}
	
	public TYPE(String initialValue) {
		this.setValue(initialValue);
	}
	
	
			/* --- METHODS --- */
	public abstract void setValue(String value);
	
	public abstract boolean isEqual(TYPE type);
	
	public boolean hasValue() {
		return this.value != null;
	}
	
	public T getValue() {
		return this.value;
	}
	
	public static TYPE fromToken(Token token) {
		// TODO Implement other types
		if (token.type() == TokenType.INT) {
			return new INT();
		}
		else if (token.type() == TokenType.BOOL) {
			return new BOOL();
		}
		else return null;
	}
	
	public abstract String typeString();
	
	
}
