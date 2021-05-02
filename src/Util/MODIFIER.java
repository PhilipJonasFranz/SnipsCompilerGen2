package Util;

/**
 * Denotes how accessible and visible a ressource is.
 */
public enum MODIFIER {
	
	/**
	 * The ressource can be accessed from anywhere. Even a struct-nested ressource
	 * can be accessed without a struct instance.
	 */
	STATIC, 
	
	/**
	 * The ressource is shared and can be accessed from anywhere. Struct-Nested
	 * ressources require a struct instance to access the ressource.
	 */
	SHARED, 
	
	/**
	 * The ressource is only accessible from within the same namespace and sub-namespace.
	 */
	RESTRICTED, 
	
	/**
	 * The ressourcee is only accessible from the same namespace.
	 */
	EXCLUSIVE;
	
}
