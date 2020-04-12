package Imm.TYPE;

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
	
}
