package Imm.TYPE;

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
	
	public abstract String typeString();
	
	public abstract String sourceCodeRepresentation();
	
}
