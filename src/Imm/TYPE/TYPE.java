package Imm.TYPE;

import java.util.List;

import Imm.TYPE.PRIMITIVES.PRIMITIVE;
import Par.Token;
import Par.Token.TokenType;
import Res.Const;
import Util.Logging.LogPoint;
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
	
	/**
	 * Check if this type is equal to the given type.
	 */
	public abstract boolean isEqual(TYPE type);
	
	public boolean hasValue() {
		return this.value != null;
	}
	
	/**
	 * Create a pretty-print version of this type and the sub-types.
	 */
	public abstract String typeString();
	
	public abstract String sourceCodeRepresentation();
	
	/**
	 * Returns the size of this type in datawords. All primitive types are f.E 1 word large,
	 * an array is [Element Size] * [Size] large.
	 */
	public abstract int wordsize();
	
	/**
	 * Returns the core type of this type. The core type is defined as 'most inner type'. For example:<br>
	 * <br> The core type of INT is INT.
	 * <br> The core type of INT* is INT.
	 * <br> The core type of INT [2] is INT.
	 * <br> The core type of STRUCT is STRUCT.
	 * @return A reference to the core type.
	 */
	public abstract TYPE getCoreType();
	
	public static TYPE fromToken(Token token, List<Message> buffered) {
		if (token.type() == TokenType.PROVISO) {
			return new PROVISO(token.spelling());
		}
		else {
			TYPE t = PRIMITIVE.fromToken(token);
			
			if (t == null) {
				t = new PROVISO(token.spelling());
				buffered.add(new Message(String.format(Const.UNKNOWN_TYPE, token.spelling()), LogPoint.Type.WARN, true));
			}
			
			return t;
		}
	}
	
	/**
	 * Creates a disjunct copy of this type.
	 */
	public abstract TYPE clone();
	
	/**
	 * Returns the value of this type. Used to get the value of an enum-type
	 * or get the value from an atom.
	 */
	public T getValue() {
		return this.value;
	}
	
	/**
	 * Returns a copy of this type, but for every proviso contained within this or
	 * any subtype, the proviso is switched out with its context. This results in a
	 * proviso-tree type structure. 
	 * @return The new resulting type.
	 * @throws SNIPS_EXC If at one point there is a proviso type that has no context.
	 */
	public abstract TYPE provisoFree();
	
	/**
	 * Remap the proviso with the given name to the given type.
	 * Remapping happens if the call reaches a proviso type whiches placeholder name
	 * matches the given name. In this case, the newType is returned. In any other case,
	 * the original type is returned.
	 */
	public abstract TYPE remapProvisoName(String name, TYPE newType);
	
	public abstract TYPE mappable(TYPE mapType, String searchedProviso);
	
} 
