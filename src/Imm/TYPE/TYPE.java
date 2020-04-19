package Imm.TYPE;

import Exc.PARSE_EXCEPTION;
import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Par.Token;
import Par.Token.TokenType;
import Util.Logging.Message;
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
	
	public static TYPE fromToken(Token token) throws PARSE_EXCEPTION {
		if (token.type() == TokenType.PROVISO) {
			return new PROVISO(token.spelling);
		}
		else {
			TYPE t = PRIMITIVE.fromToken(token);
			
			if (t == null) {
				t = new PROVISO(token.spelling);
				new Message("Unknown Type '" + token.spelling + "', creating Proviso", Message.Type.WARN);
			}
			
			return t;
		}
	}
	
	public abstract TYPE clone();
	
}
