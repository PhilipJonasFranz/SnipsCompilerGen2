package Imm.TYPE;

import Exc.SNIPS_EXC;

public abstract class TYPE<T> {

			/* ---< FIELDS >--- */
	/** The size of this type in datawords. */
	protected int wordSize = 1;
	
	/** 
	 * The value stored in this particular type instance. 
	 * Used for attatching values to atoms. 
	 */
	public T value;
	
	
			/* ---< CONSTRUCTORS >--- */
	/**
	 * Default constructor, creates new type without value.
	 */
	public TYPE() {
		
	}
	
	
			/* ---< METHODS >--- */
	/**
	 * Sets the value of this type. Types extending from this type use different parsing
	 * methods to convert the given string to a parameterized value format.
	 * @param value The value to set to this type.
	 */
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
	
	/**
	 * Returns the value of this type in a form that can be written into the generated assembly
	 * file. For example, an int would just return the stored number, a char would return the UTF-8 value
	 * of the stored character.
	 */
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
	 * Return true iff this or any of the subtypes contains a proviso.
	 */
	public abstract boolean hasProviso();
	
	/**
	 * Remap the proviso with the given name to the given type.
	 * Remapping happens if the call reaches a proviso type whiches placeholder name
	 * matches the given name. In this case, the newType is returned. In any other case,
	 * the original type is returned.
	 */
	public abstract TYPE remapProvisoName(String name, TYPE newType);
	
	public abstract TYPE mappable(TYPE mapType, String searchedProviso);
	
} 
