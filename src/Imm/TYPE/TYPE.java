package Imm.TYPE;

import java.util.List;

import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Par.Token;
import Par.Token.TokenType;
import Util.Logging.Message;

public abstract class TYPE<T> {

			/* --- FIELDS --- */
	protected int wordSize = 1;
	
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
	
	public abstract String typeString();
	
	public abstract String sourceCodeRepresentation();
	
	public abstract int wordsize();
	
	public abstract TYPE getCoreType();
	
	public static TYPE fromToken(Token token, List<Message> buffered) {
		if (token.type() == TokenType.PROVISO) {
			return new PROVISO(token.spelling);
		}
		else {
			TYPE t = PRIMITIVE.fromToken(token);
			
			if (t == null) {
				t = new PROVISO(token.spelling);
				buffered.add(new Message("Unknown Type '" + token.spelling + "', creating Proviso", Message.Type.WARN, true));
			}
			
			return t;
		}
	}
	
	public abstract TYPE clone();
	
	public T getValue() {
		return this.value;
	}
	
}
